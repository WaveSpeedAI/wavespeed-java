package ai.wavespeed;

import ai.wavespeed.openapi.client.ApiException;
import ai.wavespeed.openapi.client.model.Prediction;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        WaveSpeed waveSpeed = new WaveSpeed();
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("enable_base64_output", true);
        input.put("enable_safety_checker", true);
        input.put("guidance_scale", 3.5);
        input.put("num_images", 1);
        input.put("num_inference_steps", 28);
        input.put("prompt", "Girl in red dress, hilltop, white deer, rabbits, sunset, japanese anime style");
        input.put("seed", -1);
        input.put("size", "1024*1024");
        input.put("strength", 0.8);

        try {
            System.out.println(input);
            Prediction prediction = waveSpeed.run("wavespeed-ai/flux-dev", input);
            System.out.println(prediction);

            Prediction prediction2 = waveSpeed.create("wavespeed-ai/flux-dev", input);
            while (prediction2.getStatus() != Prediction.StatusEnum.COMPLETED && prediction2.getStatus() != Prediction.StatusEnum.FAILED) {
                Thread.sleep(2000);
                System.out.println("query status: " + prediction2.getStatus());
                prediction2 = waveSpeed.getPrediction(prediction2.getId());
            }
            System.out.println(prediction2);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
}
