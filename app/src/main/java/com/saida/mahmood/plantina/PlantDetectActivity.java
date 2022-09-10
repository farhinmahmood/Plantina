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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    private HashMap<String,String> tagMap;
    LottieAnimationView imageScan;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_detect);
        getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        tagMap= new HashMap<String,String>();
        imageScan = findViewById(R.id.imageScanLottie);
        imageScan.setVisibility(View.GONE);
        imageIv = findViewById(R.id.imageIv);
        webSearchTv = findViewById(R.id.webSearchTv);
        plantTv = findViewById(R.id.plantTv);
        startTv = findViewById(R.id.searchTv);
        bitmap =  image;
        imageIv.setImageBitmap(bitmap);
        labelFix();

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
                    String temp= "";
                    for(Map.Entry m : tagMap.entrySet()){
                        if(diseaseStr.equalsIgnoreCase((String) m.getKey())){
                            temp = (String) m.getValue();
                            break;
                        }
                    }
                    String url = "https://en.wikipedia.org/wiki/"+temp;
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }
            }
        });

    }

    private void labelFix() {
        tagMap.put("Apple___Apple_scab","Apple_scab");
        tagMap.put("Apple___Black_rot","Black_rot");
        tagMap.put("Apple___Cedar_apple_rust","Gymnosporangium_juniperi-virginianae");
        tagMap.put("Apple___healthy","Apple");
        tagMap.put("Blueberry___healthy","Blueberry");
        tagMap.put("Cherry_(including_sour)___healthy","Cherry");
        tagMap.put("Cherry_(including_sour)___Powdery_mildew","Powdery_mildew");
        tagMap.put("Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot","Corn_grey_leaf_spot");
        tagMap.put("Corn_(maize)___Common_rust_","Puccinia_sorghi");
        tagMap.put("Corn_(maize)___healthy","Maize");
        tagMap.put("Corn_(maize)___Northern_Leaf_Blight","Northern_corn_leaf_blight");
        tagMap.put("Grape___Black_rot","Black_rot_(grape_disease)");
        tagMap.put("Grape___Esca_(Black_Measles)","Esca_(grape_disease)");
        tagMap.put("Grape___healthy","Grape");
        tagMap.put("Grape___Leaf_blight_(Isariopsis_Leaf_Spot)","Isariopsis");
        tagMap.put("Orange___Haunglongbing_(Citrus_greening)","Citrus_greening_disease");
        tagMap.put("Peach___Bacterial_spot","Bacterial_leaf_scorch");
        tagMap.put("Peach___healthy","Peach");
        tagMap.put("Pepper,_bell___Bacterial_spot","Xanthomonas_campestris_pv._vesicatoria");
        tagMap.put("Pepper,_bell___healthy","Bell_pepper");
        tagMap.put("Potato___Early_blight","Potato_blight");
        tagMap.put("Potato___healthy","Potato");
        tagMap.put("Potato___Late_blight","Phytophthora_infestans");
        tagMap.put("Raspberry___healthy","Raspberry");
        tagMap.put("Soybean___healthy","Soybean");
        tagMap.put("Squash___Powdery_mildew","Powdery_mildew");
        tagMap.put("Strawberry___healthy","Strawberry");
        tagMap.put("Strawberry___Leaf_scorch","Diplocarpon_earlianum");
        tagMap.put("Tomato___Bacterial_spot","Xanthomonas_campestris_pv._vesicatoria");
        tagMap.put("Tomato___Early_blight","Alternaria_solani");
        tagMap.put("Tomato___healthy","Tomato");
        tagMap.put("Tomato___Late_blight","Phytophthora_infestans");
        tagMap.put("Tomato___Leaf_Mold","Tomato_leaf_mold");
        tagMap.put("Tomato___Septoria_leaf_spot","Septoria_lycopersici");
        tagMap.put("Tomato___Spider_mites Two-spotted_spider_mite","Tetranychus_urticae");
        tagMap.put("Tomato___Target_Spot","Corynespora_cassiicola");
        tagMap.put("Tomato___Tomato_mosaic_virus","Tomato_mosaic_virus");
        tagMap.put("Tomato___Tomato_Yellow_Leaf_Curl_Virus","Tomato_yellow_leaf_curl_virus");

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