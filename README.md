# YOLOv8 Android App - TFLite

This repository contains an Android application utilizing a TensorFlow Lite model based on YOLOv8 for object detection.

## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Model Conversion](#model-conversion)
- [Model Details](#model-details)
- [TensorFlow Lite Integration](#tensorflow-lite-integration)
- [Creating Your Own YOLOv8 Model](#creating-your-own-yolov8-model)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgements](#acknowledgements)

## Introduction

The YOLOv8 Android App is a mobile application designed for real-time object detection using the YOLOv8 model. This project exemplifies the integration of TensorFlow Lite (TFLite) with an Android application to deliver efficient and accurate object detection on mobile devices.

## Features

- Real-time object detection powered by YOLOv8
- Efficient processing with TensorFlow Lite
- Intuitive and user-friendly interface

## Installation

### Prerequisites

- Android Studio
- An Android device or emulator

### Steps

1. **Clone the repository:**
    ```bash
    git clone https://github.com/superbabii/YOLOv8-AndroidApp.git
    cd YOLOv8-AndroidApp
    ```

2. **Open the project in Android Studio:**
    - Launch Android Studio and select `Open an existing Android Studio project`.
    - Navigate to the cloned repository and open it.

3. **Build and run the project:**
    - Connect your Android device or start an emulator.
    - Click `Run` in Android Studio.

## Usage

1. Launch the app on your device.
2. Aim your camera at objects to detect them in real-time.
3. Detected objects will be highlighted with bounding boxes and labels.

## Model Conversion

To convert the YOLOv8 model to TensorFlow Lite, follow the steps outlined in the `model_conversion.ipynb` notebook available on Google Drive.

### Steps

1. **Open the notebook in Google Colab:**
    - Click the link to open the notebook: [model_conversion.ipynb](https://colab.research.google.com/drive/10RQCjBIc19sna2Nwa4oGso1aC17W8ARq?usp=sharing).

2. **Set up the environment:**
    The notebook will automatically install the necessary dependencies.

3. **Run the notebook:**
    Follow the instructions in the notebook to:
    - Export the YOLOv8 model.
    - Convert the exported model to TensorFlow Lite format.

4. **Download the TFLite model:**
    Once the conversion is complete, download the `.tflite` file.

5. **Move the TFLite model to the Android project:**
    - Navigate to the Android project directory: `app/src/main/assets/`.
    - Replace the existing `model.tflite` file with the downloaded `.tflite` file.

   * The default model in `app/src/main/assets/` is converted from [yolov8n.pt](https://github.com/ultralytics/assets/releases/download/v8.2.0/yolov8n.pt).

## Model Details

The app employs a pre-trained YOLOv8 model converted to TensorFlow Lite format. YOLOv8 is renowned for its balance between speed and accuracy, making it ideal for mobile applications.

## TensorFlow Lite Integration

The TensorFlow Lite model's input and output tensor formats are as follows:
- **Input tensor:** float32[1, 640, 640, 3] or float32[1, 3, 640, 640]
- **Output tensor:** float32[1, 84, 8400]

This integration eliminates concerns related to the tensor format or metadata issues previously encountered with [TensorFlow Lite's object detection example for Android](https://github.com/tensorflow/examples/tree/master/lite/examples/object_detection/android).

## Creating Your Own YOLOv8 Model

You can also customize the YOLOv8 model to suit your specific needs. For instance, you can use the VisDrone dataset to create a YOLOv8 model. Please refer to the [VisDrone YOLOv8 Models Upgrade](https://github.com/superbabii/VisDrone-YOLOv8-Models-Upgrade) repository for detailed instructions.

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/YourFeature`).
3. Commit your changes (`git commit -m 'Add some feature'`).
4. Push to the branch (`git push origin feature/YourFeature`).
5. Create a new Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgements

- [YOLOv8](https://github.com/ultralytics/ultralytics) for the object detection model.
- [TensorFlow Lite](https://www.tensorflow.org/lite) for providing the framework to run the model on mobile devices.
- [Android Studio](https://developer.android.com/studio) for the development environment.
- [Google Colab](https://colab.research.google.com) for providing a cloud-based Jupyter notebook environment.
