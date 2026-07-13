import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { ApiRequestError, ocrAssistedCheckIn, recognizePlate, type OcrResult, type ParkingCheckInResponse } from "../api/visionApi";
import { isPlateInCooldown, pruneExpiredPlateCooldowns, rememberLockedPlate } from "../camera/gateEntryCooldown";
import { buildManualCheckInPayload, buildOcrCheckInPayload, customerTicketUrl, GateEntryState, retryBackoffMs, SCAN_COOLDOWN_MS, shouldLockCandidate } from "../camera/gateEntryState";

type EntryMode = "CAMERA" | "MANUAL";

const stateCopy: Record<GateEntryState, string> = {
  CAMERA_IDLE: "Camera is ready to start.",
  CAMERA_STARTING: "Đang bật camera...",
  SCANNING: "Đang chờ biển số...",
  RECOGNIZING: "Đang nhận diện...",
  PLATE_READY: "Đã nhận diện biển số, vui lòng kiểm tra rồi nhấn Enter.",
  CHECKING_IN: "Đang check-in...",
  QR_READY: "Check-in thành công. Đưa QR này cho khách.",
  ERROR: "Lỗi camera / Lỗi nhận diện / Lỗi check-in",
};
const OCR_REQUEST_TIMEOUT_MS = 10_000;

function readApiError(error: unknown): { status?: number; message: string } {
  if (error instanceof ApiRequestError) return { status: error.status, message: error.message };
  return { message: error instanceof Error ? error.message : "Request failed." };
}

function cameraErrorMessage(error: unknown): string {
  const name = error instanceof DOMException ? error.name : "";
  if (name === "NotAllowedError" || name === "SecurityError") return "Không thể mở camera. Vui lòng cấp quyền camera cho trình duyệt.";
  if (name === "NotFoundError" || name === "OverconstrainedError") return "Không tìm thấy camera.";
  return "Không thể mở camera. Vui lòng kiểm tra camera và thử lại.";
}

function checkInErrorMessage(error: unknown): string {
  const details = readApiError(error);
  const normalized = details.message.toLowerCase();
  if (details.status === 401 || details.status === 403) return "JWT không có quyền check-in. Vui lòng dùng tài khoản ADMIN hoặc PARKING_STAFF.";
  if (normalized.includes("duplicate") || normalized.includes("active") || normalized.includes("already")) return "Biển số này đang có phiên gửi xe hoạt động. Parking Service đã từ chối check-in trùng.";
  if (normalized.includes("reservation")) return "Mã đặt chỗ không khớp hoặc không còn hợp lệ. Vui lòng kiểm tra lại.";
  return "Check-in chưa thành công. Vui lòng kiểm tra biển số và thử lại.";
}

function captureFrame(video: HTMLVideoElement): Promise<File> {
  return new Promise((resolve, reject) => {
    if (!video.videoWidth || !video.videoHeight) return reject(new Error("Camera preview is not ready."));
    const canvas = document.createElement("canvas");
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const context = canvas.getContext("2d");
    if (!context) return reject(new Error("Camera frame capture is unavailable."));
    context.drawImage(video, 0, 0, canvas.width, canvas.height);
    canvas.toBlob((blob) => blob ? resolve(new File([blob], "camera-frame.jpg", { type: "image/jpeg" })) : reject(new Error("Could not capture a frame.")), "image/jpeg", 0.88);
  });
}

export function StaffGateEntryPage() {
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const scanTimerRef = useRef<number | null>(null);
  const scanGenerationRef = useRef(0);
  const requestInFlightRef = useRef(false);
  const checkInInFlightRef = useRef(false);
  const abortControllerRef = useRef<AbortController | null>(null);
  const consecutiveFailuresRef = useRef(0);
  const recentlyLockedPlatesRef = useRef(new Map<string, number>());
  const triggerScanRef = useRef<() => void>(() => undefined);
  const [state, setState] = useState<GateEntryState>("CAMERA_IDLE");
  const [mode, setMode] = useState<EntryMode>("CAMERA");
  const [scanDelayMs, setScanDelayMs] = useState(SCAN_COOLDOWN_MS);
  const [token, setToken] = useState("");
  const [confirmedPlate, setConfirmedPlate] = useState("");
  const [vehicleType, setVehicleType] = useState<"MOTORBIKE" | "CAR">("MOTORBIKE");
  const [entryGate, setEntryGate] = useState("GATE_IN_01");
  const [reservationCode, setReservationCode] = useState("");
  const [ocr, setOcr] = useState<OcrResult | null>(null);
  const [session, setSession] = useState<ParkingCheckInResponse | null>(null);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  const cancelRecognition = useCallback(() => {
    scanGenerationRef.current += 1;
    abortControllerRef.current?.abort();
    abortControllerRef.current = null;
    if (scanTimerRef.current !== null) window.clearTimeout(scanTimerRef.current);
    scanTimerRef.current = null;
  }, []);

  const stopStream = useCallback(() => {
    cancelRecognition();
    const stream = streamRef.current;
    streamRef.current = null;
    if (videoRef.current) videoRef.current.srcObject = null;
    stream?.getTracks().forEach((track) => track.stop());
  }, [cancelRecognition]);

  useEffect(() => () => stopStream(), [stopStream]);

  const resetForScan = useCallback((clearCooldown = false) => {
    cancelRecognition();
    if (clearCooldown) recentlyLockedPlatesRef.current.clear();
    consecutiveFailuresRef.current = 0;
    setMode("CAMERA");
    setSession(null);
    setOcr(null);
    setConfirmedPlate("");
    setError("");
    setNotice("");
    setScanDelayMs(SCAN_COOLDOWN_MS);
    setState(streamRef.current && token.trim() ? "SCANNING" : "CAMERA_IDLE");
  }, [cancelRecognition, token]);

  const startCamera = useCallback(async () => {
    stopStream();
    setMode("CAMERA");
    setError("");
    setNotice("");
    setSession(null);
    setOcr(null);
    setConfirmedPlate("");
    setState("CAMERA_STARTING");
    try {
      if (!navigator.mediaDevices?.getUserMedia) throw new DOMException("Camera API unavailable", "NotFoundError");
      const stream = await navigator.mediaDevices.getUserMedia({ audio: false, video: { facingMode: { ideal: "environment" }, width: { ideal: 1280 }, height: { ideal: 720 } } });
      streamRef.current = stream;
      const onTrackEnded = () => {
        if (streamRef.current !== stream) return;
        stopStream();
        setError("Camera stream đã ngắt. Vui lòng khởi động lại camera.");
        setState("ERROR");
      };
      stream.getVideoTracks().forEach((track) => track.addEventListener("ended", onTrackEnded, { once: true }));
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      setScanDelayMs(SCAN_COOLDOWN_MS);
      setState(token.trim() ? "SCANNING" : "CAMERA_IDLE");
      if (!token.trim()) setNotice("Paste an ADMIN or PARKING_STAFF JWT to start recognition.");
    } catch (cameraError) {
      stopStream();
      setError(cameraErrorMessage(cameraError));
      setState("ERROR");
    }
  }, [stopStream, token]);

  const stopCamera = useCallback(() => {
    stopStream();
    setNotice("");
    setState("CAMERA_IDLE");
  }, [stopStream]);

  const scanCurrentFrame = useCallback(async () => {
    if (state !== "SCANNING" || mode !== "CAMERA" || !streamRef.current) return;
    if (requestInFlightRef.current) {
      scanTimerRef.current = window.setTimeout(() => triggerScanRef.current(), scanDelayMs);
      return;
    }
    if (!token.trim()) {
      setError("Paste an ADMIN or PARKING_STAFF JWT before scanning.");
      setState("ERROR");
      return;
    }
    const generation = scanGenerationRef.current;
    const controller = new AbortController();
    let timedOut = false;
    const timeoutId = window.setTimeout(() => {
      timedOut = true;
      controller.abort();
    }, OCR_REQUEST_TIMEOUT_MS);
    abortControllerRef.current = controller;
    requestInFlightRef.current = true;
    setState("RECOGNIZING");
    try {
      const video = videoRef.current;
      if (!video) throw new Error("Camera preview unavailable.");
      const result = await recognizePlate(token.trim(), await captureFrame(video), controller.signal);
      if (generation !== scanGenerationRef.current) return;
      if (shouldLockCandidate(result)) {
        const plate = result.normalizedCandidatePlate ?? result.candidatePlate;
        const now = Date.now();
        pruneExpiredPlateCooldowns(recentlyLockedPlatesRef.current, now);
        if (isPlateInCooldown(recentlyLockedPlatesRef.current, plate, now)) {
          setNotice("Biển số này vừa được quét. Vui lòng bấm Next Vehicle hoặc chờ xe rời vùng quét.");
          setScanDelayMs(SCAN_COOLDOWN_MS);
          setState("SCANNING");
          return;
        }
        rememberLockedPlate(recentlyLockedPlatesRef.current, plate, now);
        consecutiveFailuresRef.current = 0;
        setScanDelayMs(SCAN_COOLDOWN_MS);
        setOcr(result);
        setConfirmedPlate(result.candidatePlate ?? "");
        setNotice(result.warnings.join(" "));
        setState("PLATE_READY");
        return;
      }
      consecutiveFailuresRef.current = 0;
      setScanDelayMs(SCAN_COOLDOWN_MS);
      setNotice(result.candidatePlate ? "Độ tin cậy thấp, đang thử lại." : "Chưa nhận diện được biển số, đang thử lại.");
      setState("SCANNING");
    } catch (scanError) {
      if (generation !== scanGenerationRef.current || (!timedOut && scanError instanceof DOMException && scanError.name === "AbortError")) return;
      const details = readApiError(scanError);
      if (details.status === 401 || details.status === 403) {
        setError("JWT không có quyền OCR. Vui lòng dùng tài khoản ADMIN hoặc PARKING_STAFF.");
        setState("ERROR");
      } else if (details.status === 503) {
        setError("Vision provider chưa được cấu hình. Kiểm tra GEMINI_API_KEY hoặc chọn DEMO_OCR.");
        setState("ERROR");
      } else {
        consecutiveFailuresRef.current += 1;
        const backoff = retryBackoffMs(consecutiveFailuresRef.current);
        setScanDelayMs(backoff);
        setNotice("Vision service tạm thời không khả dụng. Đang thử lại sau ít giây.");
        setState("SCANNING");
      }
    } finally {
      window.clearTimeout(timeoutId);
      if (abortControllerRef.current === controller) abortControllerRef.current = null;
      requestInFlightRef.current = false;
    }
  }, [mode, scanDelayMs, state, token]);

  triggerScanRef.current = () => void scanCurrentFrame();

  useEffect(() => {
    if (state !== "SCANNING" || mode !== "CAMERA") return;
    scanTimerRef.current = window.setTimeout(() => void scanCurrentFrame(), scanDelayMs);
    return () => {
      if (scanTimerRef.current !== null) window.clearTimeout(scanTimerRef.current);
      scanTimerRef.current = null;
    };
  }, [mode, scanCurrentFrame, scanDelayMs, state]);

  function activateManualFallback() {
    cancelRecognition();
    setMode("MANUAL");
    setOcr(null);
    setConfirmedPlate("");
    setError("");
    setNotice("Chế độ nhập biển số thủ công đang hoạt động. Camera/OCR không được dùng cho lần check-in này.");
    setState("PLATE_READY");
  }

  function scanAgain() {
    resetForScan(true);
  }

  function clearCooldown() {
    recentlyLockedPlatesRef.current.clear();
    setNotice("Đã xóa bộ nhớ biển số vừa quét.");
  }

  async function confirmCheckIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (checkInInFlightRef.current || state !== "PLATE_READY" || !confirmedPlate.trim() || (mode === "CAMERA" && !ocr)) return;
    if (!token.trim()) {
      setError("Paste an ADMIN or PARKING_STAFF JWT before check-in.");
      return;
    }
    checkInInFlightRef.current = true;
    setError("");
    setState("CHECKING_IN");
    try {
      const payload = mode === "MANUAL"
        ? buildManualCheckInPayload({ confirmedPlate, vehicleType, entryGate, reservationCode })
        : buildOcrCheckInPayload({ confirmedPlate, vehicleType, entryGate, reservationCode, ocr: ocr! });
      const result = await ocrAssistedCheckIn(token.trim(), payload);
      setSession(result);
      setState("QR_READY");
    } catch (checkInError) {
      setError(checkInErrorMessage(checkInError));
      setState("PLATE_READY");
    } finally {
      checkInInFlightRef.current = false;
    }
  }

  function nextVehicle() {
    cancelRecognition();
    setMode("CAMERA");
    setSession(null);
    setOcr(null);
    setConfirmedPlate("");
    setReservationCode("");
    setError("");
    setNotice("Vui lòng cho xe hiện tại rời khỏi vùng camera trước khi quét xe tiếp theo.");
    setScanDelayMs(SCAN_COOLDOWN_MS);
    setState(streamRef.current && token.trim() ? "SCANNING" : "CAMERA_IDLE");
  }

  async function copyTicketLink(url: string) {
    try {
      await navigator.clipboard.writeText(url);
      setNotice("Đã sao chép liên kết vé cho khách.");
    } catch {
      setNotice("Không thể sao chép trong trình duyệt này. Vui lòng chọn liên kết bên dưới.");
    }
  }

  const ticketUrl = session?.qrLookupToken ? customerTicketUrl(session.qrLookupToken) : session?.ticketUrl;
  const isCameraRunning = streamRef.current !== null;
  const canConfirm = Boolean(state === "PLATE_READY" && confirmedPlate.trim() && (mode === "MANUAL" || ocr !== null));

  return <main className="gate-entry-layout">
    <section className="gate-entry-heading">
      <div><span className="ticket-code">Staff gate entry</span><h1>Live camera check-in</h1><p>OCR only suggests a plate. Staff must confirm it before a normal parking session is created.</p></div>
      <span className={`gate-state gate-state--${state.toLowerCase()}`}>{mode === "MANUAL" ? "Nhập biển số thủ công" : stateCopy[state]}</span>
    </section>

    <section className="gate-entry-grid">
      <div className="camera-station">
        <div className="camera-preview" aria-label="Live vehicle plate camera preview"><video ref={videoRef} autoPlay muted playsInline /><div className="plate-guide" aria-hidden="true"><span>Plate guide</span></div>{!isCameraRunning && <div className="camera-empty"><strong>Camera preview</strong><span>Start the camera to begin live plate recognition.</span></div>}</div>
        <div className="camera-actions"><button type="button" className="primary-button" onClick={() => void startCamera()} disabled={state === "CAMERA_STARTING" || state === "CHECKING_IN"}>Start Camera</button><button type="button" className="secondary-button" onClick={stopCamera} disabled={!isCameraRunning}>Stop Camera</button>{mode === "CAMERA" && state === "PLATE_READY" && <button type="button" className="secondary-button" onClick={scanAgain}>Scan Again</button>}<button type="button" className="secondary-button" onClick={clearCooldown}>Clear cooldown</button></div>
        <p className="camera-privacy">Frames are transient, used only for the current OCR request, and never stored.</p>
      </div>

      <div className="gate-control">
        <label>Staff JWT<textarea value={token} onChange={(event) => setToken(event.target.value)} rows={3} placeholder="Paste PARKING_STAFF or ADMIN JWT" /></label>
        <div className="manual-mode-actions">{mode === "MANUAL" ? <button type="button" className="secondary-button" onClick={() => resetForScan(false)}>Quay lại quét camera</button> : <button type="button" className="secondary-button" onClick={activateManualFallback}>Nhập biển số thủ công</button>}</div>
        <form onSubmit={(event) => void confirmCheckIn(event)}>
          <fieldset disabled={state === "CHECKING_IN" || state === "QR_READY"}>
            <legend>{mode === "MANUAL" ? "Check-in thủ công" : "Confirm check-in"}</legend>
            <label>Confirmed plate<input value={confirmedPlate} onChange={(event) => setConfirmedPlate(event.target.value)} placeholder={mode === "MANUAL" ? "Nhập biển số" : "Waiting for a plate candidate"} required /></label>
            <div className="gate-fields"><label>Vehicle type<select value={vehicleType} onChange={(event) => setVehicleType(event.target.value as "MOTORBIKE" | "CAR")}><option value="MOTORBIKE">Motorbike</option><option value="CAR">Car</option></select></label><label>Entry gate<input value={entryGate} onChange={(event) => setEntryGate(event.target.value)} required /></label></div>
            <label>Reservation code <span>(optional)</span><input value={reservationCode} onChange={(event) => setReservationCode(event.target.value)} /></label>
            {ocr && mode === "CAMERA" && <div className="ocr-readout"><p><strong>{ocr.provider}</strong> · {(ocr.confidence * 100).toFixed(0)}% confidence</p><p>OCR candidate: <strong>{ocr.candidatePlate ?? "No candidate"}</strong></p>{ocr.warnings.map((warning) => <p key={warning}>{warning}</p>)}</div>}
            <button className="primary-button" type="submit" disabled={!canConfirm || checkInInFlightRef.current}>{state === "CHECKING_IN" ? "Checking in..." : "Confirm Check-in"}</button>
          </fieldset>
        </form>
        {notice && <p className="gate-notice" role="status">{notice}</p>}
        {error && <p className="payment-error" role="alert">{error}</p>}
      </div>
    </section>

    {state === "QR_READY" && session && <section className="gate-ticket" aria-live="polite"><div><span className="ticket-code">Customer ticket</span><h2>Check-in confirmed</h2><p><strong>{session.sessionCode}</strong> · {session.vehiclePlate}</p><p>{session.entryTime ? new Date(session.entryTime).toLocaleString() : "Entry time recorded"} · {session.paymentStatus ?? "UNPAID"}</p></div><div className="ticket-link-box"><span>Customer ticket link</span><code>{ticketUrl}</code><div><button type="button" className="secondary-button" onClick={() => ticketUrl && void copyTicketLink(ticketUrl)}>Copy link</button>{ticketUrl && <a className="primary-button" href={ticketUrl} target="_blank" rel="noreferrer">Open Ticket</a>}</div></div>{session.qrLookupToken && <p className="qr-token">Lookup token: <code>{session.qrLookupToken}</code></p>}<p className="security-notice">This QR ticket lets the customer view the ticket. It does not authorize exit.</p><button type="button" className="primary-button" onClick={nextVehicle}>Next Vehicle</button></section>}
  </main>;
}
