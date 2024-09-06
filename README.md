# TensorFlow to PyTorch Project

This repository contains an Android application utilizing a PyTorch model converted from TensorFlow for object detection.

## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Model Conversion](#model-conversion)
- [Model Details](#model-details)
- [PyTorch Integration](#pytorch-integration)
- [Creating Your Own PyTorch Model](#creating-your-own-pytorch-model)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgements](#acknowledgements)

## Introduction

The **TensorFlow to PyTorch Project** is a mobile application designed for real-time object detection using a PyTorch model converted from TensorFlow. This project demonstrates the integration of PyTorch with an Android application to deliver efficient and accurate object detection on mobile devices.

## Features

- Real-time object detection powered by PyTorch
- Efficient processing with PyTorch on Android
- Intuitive and user-friendly interface

## Installation

### Prerequisites

- Android Studio
- An Android device or emulator

### Steps

1. **Clone the repository:**
    ```bash
    git clone https://github.com/superbabii/TensorFlow-to-PyTorch-Project.git
    cd TensorFlow-to-PyTorch-Project
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

To convert a TensorFlow model to PyTorch, follow the steps outlined in the `model_conversion.ipynb` notebook available on Google Drive.

### Steps

1. **Open the notebook in Google Colab:**
    - Click the link to open the notebook: [model_conversion.ipynb](https://colab.research.google.com/drive/10RQCjBIc19sna2Nwa4oGso1aC17W8ARq?usp=sharing).

2. **Set up the environment:**
    The notebook will automatically install the necessary dependencies.

3. **Run the notebook:**
    Follow the instructions in the notebook to:
    - Export the TensorFlow model.
    - Convert the exported model to PyTorch format.

4. **Download the PyTorch model:**
    Once the conversion is complete, download the `.pt` file.

5. **Move the PyTorch model to the Android project:**
    - Navigate to the Android project directory: `app/src/main/assets/`.
    - Replace the existing `model.pt` file with the downloaded `.pt` file.

## Model Details

The app employs a pre-trained TensorFlow model converted to PyTorch format, ensuring high-speed and accurate detection for mobile applications.

## PyTorch Integration

The PyTorch model's input and output tensor formats are as follows:
- **Input tensor:** float32[1, 640, 640, 3]
- **Output tensor:** float32[1, 84, 8400]

This integration avoids concerns related to the tensor format or metadata issues.

## Creating Your Own PyTorch Model

You can also customize the PyTorch model to suit your specific needs. For instance, you can use the VisDrone dataset to create a PyTorch model. Please refer to the [VisDrone PyTorch Models Upgrade](https://github.com/superbabii/VisDrone-PyTorch-Models-Upgrade) repository for detailed instructions.

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

- [TensorFlow](https://www.tensorflow.org) and [PyTorch](https://pytorch.org) for the object detection models.
- [Android Studio](https://developer.android.com/studio) for the development environment.
- [Google Colab](https://colab.research.google.com) for providing a cloud-based Jupyter notebook environment.
