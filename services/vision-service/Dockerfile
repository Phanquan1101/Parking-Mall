FROM python:3.11-slim
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends libgomp1 libgl1 libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
# PaddleOCR 2.7/OpenCV wheels require the NumPy 1.x ABI on Python 3.11. The
# PDF helper installs an incompatible OpenCV 5 headless wheel, so retain only
# the matching OpenCV 4 contrib wheel used by the OCR runtime.
RUN pip uninstall -y opencv-python-headless opencv-python \
    && pip install --no-cache-dir --force-reinstall numpy==1.26.4 opencv-contrib-python==4.6.0.66
COPY app ./app
EXPOSE 8090
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]
