package com.example.nomonia;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.Image;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;


import java.io.IOException;
import java.nio.MappedByteBuffer;


public class Classifier {

    public enum Device {
        CPU,
        GPU
    }

    private MappedByteBuffer tfliteModel;
    private int imageSizeX;
    private int imageSizeY;

    private GpuDelegate gpuDelegate = null;

    private Interpreter tflite;

    private final Interpreter.Options tfliteOptions = new Interpreter.Options();

    private TensorImage inputImageBuffer;
    private TensorBuffer outputProbabilitybuffer;
    private TensorProcessor probabilityProcessor;

    private String getModelpath() {
        return "model.tflite";
    }



    protected Classifier(Activity activity, Device device, int numThreads)
        throws IOException {
        tfliteModel = FileUtil.loadMappedFile(activity, getModelpath());
        switch (device) {
            case GPU:
                gpuDelegate = new GpuDelegate();
                tfliteOptions.addDelegate(gpuDelegate);
                break;
            case CPU:
                break;
        }
        tfliteOptions.setNumThreads(numThreads);

        tflite = new Interpreter(tfliteModel, tfliteOptions);

        int imageTensorIndex = 0;
        int [] imageShape = tflite.getInputTensor(imageTensorIndex).shape();
        imageSizeX = imageShape[2];
        imageSizeY = imageShape[1];
        DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();
        int probabilityTensorIndex = 0;
        int [] probabilityShape =
                tflite.getOutputTensor(probabilityTensorIndex).shape();
        DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

        inputImageBuffer = new TensorImage(imageDataType);

        outputProbabilitybuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);

        probabilityProcessor = new TensorProcessor.Builder().build();

    }

    public String recognizeImage(final Bitmap bitmap){
        inputImageBuffer = loadImage(bitmap);
        tflite.run(inputImageBuffer.getBuffer(), outputProbabilitybuffer.getBuffer().rewind());
        float[] predictions = outputProbabilitybuffer.getFloatArray();

        Recognition recognition = new Recognition(predictions[1]);
        return recognition.toString();
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        if (gpuDelegate != null){
            gpuDelegate.close();
            gpuDelegate = null;
        }

        tfliteModel = null;
    }

    public int getImageSizeX() {return imageSizeX;}

    public int getImageSizeY() {return imageSizeY;}


    private TensorImage loadImage(Bitmap bitmap){
        inputImageBuffer.load(bitmap.copy(Bitmap.Config.ARGB_8888,true));
        int cropSize = 100;
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .build();
        return imageProcessor.process(inputImageBuffer);
    }

    public static class Recognition {
        private Float confidence;

        public Recognition(
                 Float confidence
        ){
            this.confidence = confidence;
        }

        public Float getConfidence(){return confidence;}

        @Override
        public String toString(){
            String resultString = "";

            if (confidence != null){
                resultString += String.format("(%.3f%%) ", confidence*100.0f);
            }
            return resultString;
        }
    }

}


