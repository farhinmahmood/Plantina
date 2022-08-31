package com.saida.mahmood.plantina;

import static com.saida.mahmood.plantina.TempFile.image;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PlantDetectActivity extends AppCompatActivity {
    private ImageView imageIv;
    Interpreter plantTfLite;
    private TextView plantTv, startTv, webSearchTv;
    private TensorBuffer outputProbabilityBuffer;
    private TensorProcessor probabilityProcessor;
    private TensorImage inputImageBuffer;
    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 1.0f;
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 255.0f;
    private Bitmap bitmap;
    private List<String> plantLabels;
    private  int imageSizeX;
    private  int imageSizeY;
    private String diseaseStr ="";
    LottieAnimationView imageScan;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_detect);
        getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        imageScan = findViewById(R.id.imageScanLottie);
        imageScan.setVisibility(View.GONE);
        imageIv = findViewById(R.id.imageIv);
        webSearchTv = findViewById(R.id.webSearchTv);
        plantTv = findViewById(R.id.plantTv);
        startTv = findViewById(R.id.searchTv);
        bitmap =  image;
        imageIv.setImageBitmap(bitmap);

        try {
            plantTfLite = new Interpreter(loadModelFile(this,"plantmodel.tflite"));
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        startTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageScan.setVisibility(View.VISIBLE);
                Handler handler = new Handler(Looper.getMainLooper());

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startPlantPrediction();
                    }
                }, 5000);

            }
        });
        webSearchTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(diseaseStr.isEmpty()){
                    Toast.makeText(PlantDetectActivity.this,"Please start the prediction first", Toast.LENGTH_SHORT).show();
                }else{
                    String url = "https://en.wikipedia.org/wiki/"+diseaseStr;
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }
            }
        });

    }

    private void startPlantPrediction() {

        imageScan.setVisibility(View.GONE);
        int imageTensorIndex = 0;
        int[] imageShape = plantTfLite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
        imageSizeY = imageShape[1];
        imageSizeX = imageShape[2];
        DataType imageDataType = plantTfLite.getInputTensor(imageTensorIndex).dataType();

        int probabilityTensorIndex = 0;
        int[] probabilityShape =
                plantTfLite.getOutputTensor(probabilityTensorIndex).shape(); // {1, NUM_CLASSES}
        DataType probabilityDataType = plantTfLite.getOutputTensor(probabilityTensorIndex).dataType();

        inputImageBuffer = new TensorImage(imageDataType);
        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
        probabilityProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();

        inputImageBuffer = loadImage(bitmap);

        plantTfLite.run(inputImageBuffer.getBuffer(),outputProbabilityBuffer.getBuffer().rewind());
        showPlantResult();
    }

    private void showPlantResult() {
        try{
            plantLabels = FileUtil.loadLabels(this,"plantmodel.txt");
        }catch (Exception e){
            e.printStackTrace();
        }
        Map<String, Float> labeledProbability =
                new TensorLabel(plantLabels, probabilityProcessor.process(outputProbabilityBuffer))
                        .getMapWithFloatValue();
        float maxValueInMap =(Collections.max(labeledProbability.values()));

        for (Map.Entry<String, Float> entry : labeledProbability.entrySet()) {
            if (entry.getValue()==maxValueInMap) {
                plantTv.setText(entry.getKey());
                diseaseStr = entry.getKey();
            }
        }
    }

    private TensorOperator getPreprocessNormalizeOp() {
        return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
    }
    private TensorOperator getPostprocessNormalizeOp(){
        return new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD);
    }

    private TensorImage loadImage(Bitmap bitmap){
        inputImageBuffer.load(bitmap);
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(getPreprocessNormalizeOp())
                        .build();
        return imageProcessor.process(inputImageBuffer);
    }

    private MappedByteBuffer loadModelFile(Activity activity, String str) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(str);
        FileInputStream fileInputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffSets = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return  fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffSets,declaredLength);

    }
}