package com.cotta.aulamlp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.jfree.data.xy.XYSeries;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.core.events.LearningEvent;
import org.neuroph.core.events.LearningEventListener;
import org.neuroph.core.learning.SupervisedLearning;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;

/**
 *
 * @author PedroHenrique
 */
public class NeuralNetworkStockPredictor {

    private int slidingWindowSize;
    private double learningRate;
    private String rawDataFilePath;
    private String rawDataTestingFilePath;

    private double max = 0;
    private double min = Double.MAX_VALUE;

    private double testMax = 0;
    private double testMin = Double.MAX_VALUE;

    private static int nNetworks = 10;

    private String learningDataFilePath = "input/learningData.csv";
    private String testingDataFilePath = "input/testingData.csv";
    private String neuralNetworkModelFilePath = "stockPredictor.nnet"; // duvida se sobreescreve ou nao em outra execução

    private static XYSeries series1;
    private static XYSeries series2;

    public static void main(String[] args) throws IOException {

        List<NeuralNetworkStockPredictor> networkList = new ArrayList<>();

        NeuralNetworkStockPredictor predictor = new NeuralNetworkStockPredictor(4, 0.3, "input/precipitation-in-mm-eastport-usa.csv", "input/precipitation-in-mm-eastport-usa-test.csv");
        predictor.prepareData();

        predictor.trainNetwork();

        predictor.dynamicTestNetwork();

        /*for (int i = 1; i <= nNetworks; i++) {
            NeuralNetworkStockPredictor predictor = new NeuralNetworkStockPredictor(i, 0.5, "input/rawTrainingData.csv", "input/rawTestingData.csv");
            networkList.add(predictor);
        }

        for (NeuralNetworkStockPredictor neuralNetworkStockPredictor : networkList) {
            neuralNetworkStockPredictor.prepareData();

            System.out.println("Training starting");
            neuralNetworkStockPredictor.trainNetwork();

            System.out.println("Testing network");
            double err = neuralNetworkStockPredictor.dynamicTestNetwork();

            series1.add(neuralNetworkStockPredictor.slidingWindowSize, err);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series1);

        XYLineChart chart1 = new XYLineChart();
        chart1.setDataset(dataset);

        chart1.show("Error (Y) x Sliding Window (X)");

        networkList.clear();

        for (int i = 1; i <= nNetworks; i++) {
            NeuralNetworkStockPredictor predictor = new NeuralNetworkStockPredictor(5, i / 10.0, "input/rawTrainingData.csv", "input/rawTestingData.csv");
            networkList.add(predictor);
        }

        for (NeuralNetworkStockPredictor neuralNetworkStockPredictor : networkList) {
            neuralNetworkStockPredictor.prepareData();

            System.out.println("Training starting");
            neuralNetworkStockPredictor.trainNetwork();

            System.out.println("Testing network");
            double err = neuralNetworkStockPredictor.testNetwork();

            series2.add(neuralNetworkStockPredictor.learningRate, err);
        }

        XYSeriesCollection dataset2 = new XYSeriesCollection();
        dataset2.addSeries(series2);

        XYLineChart chart2 = new XYLineChart();
        chart2.setDataset(dataset2);

        chart2.show("Error (Y) x Learning Rate (X)");*/
    }

    private NeuralNetworkStockPredictor(int slidingWindowSize, double rate, String rawDataFilePath, String rawTestingDataFilePath) {
        this.rawDataFilePath = rawDataFilePath;
        this.slidingWindowSize = slidingWindowSize;
        this.rawDataTestingFilePath = rawTestingDataFilePath;
        this.learningRate = rate;
        series1 = new XYSeries("");
        series2 = new XYSeries("");
    }

    void prepareData() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(rawDataFilePath));

        // Find the minimum and maximum values - needed for normalization
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                double crtValue = Double.valueOf(tokens[1]);
                if (crtValue > max) {
                    max = crtValue;
                }
                if (crtValue < min) {
                    min = crtValue;
                }
            }
        } finally {
            reader.close();
        }

        reader = new BufferedReader(new FileReader(rawDataFilePath));
        BufferedWriter writer = new BufferedWriter(new FileWriter(learningDataFilePath));

        // Keep a queue with slidingWindowSize + 1 values
        LinkedList<Double> valuesQueue = new LinkedList<>();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                double crtValue = Double.valueOf(line.split(",")[1]);
                // Normalize values and add it to the queue
                double normalizedValue = normalizeValue(crtValue);
                valuesQueue.add(normalizedValue);

                if (valuesQueue.size() == slidingWindowSize + 1) {
                    String valueLine = valuesQueue.toString().replaceAll("\\[|\\]", "");
                    writer.write(valueLine);
                    writer.newLine();
                    // Remove the first element in queue to make place for a new one
                    valuesQueue.removeFirst();
                }
            }
        } finally {
            reader.close();
            writer.close();
        }
    }

    void prepareTestData(int nNetworks) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(rawDataTestingFilePath));

        // Find the minimum and maximum values - needed for normalization
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                double crtValue = Double.valueOf(tokens[1]);
                if (crtValue > testMax) {
                    testMax = crtValue;
                }
                if (crtValue < testMin) {
                    testMin = crtValue;
                }
            }
        } finally {
            reader.close();
        }

        reader = new BufferedReader(new FileReader(rawDataTestingFilePath));
        BufferedWriter writer = new BufferedWriter(new FileWriter(testingDataFilePath));

        // Keep a queue with slidingWindowSize + 1 values
        LinkedList<Double> valuesQueue = new LinkedList<>();
        try {
            String line;
            while (((line = reader.readLine()) != null) && valuesQueue.size() <= nNetworks + 1) {
                double crtValue = Double.valueOf(line.split(",")[1]);
                // Normalize values and add it to the queue
                double normalizedValue = normalizeTestValue(crtValue);
                valuesQueue.add(normalizedValue);

                if (valuesQueue.size() == nNetworks + 1) {
                    String valueLine = valuesQueue.toString().replaceAll("\\[|\\]", "");
                    writer.write(valueLine);
                    writer.newLine();
                    // Remove the first element in queue to make place for a new one
                    //valuesQueue.clear();
                }
            }
        } finally {
            reader.close();
            writer.close();
        }
    }

    double normalizeValue(double input) {
        return (input - min) / (max - min) * 0.8 + 0.1;
    }

    double deNormalizeValue(double input) {
        return min + (input - 0.1) * (max - min) / 0.8;
    }

    double normalizeTestValue(double input) {
        return (input - testMin) / (testMax - testMin) * 0.8 + 0.1;
    }

    double deNormalizeTestValue(double input) {
        return testMin + (input - 0.1) * (testMax - testMin) / 0.8;
    }

    void trainNetwork() throws IOException {
        NeuralNetwork<BackPropagation> neuralNetwork = new MultiLayerPerceptron(slidingWindowSize, 2 * slidingWindowSize + 1, 1);

        int maxIterations = 1000;
        double maxError = 0.00001;

        SupervisedLearning learningRule = neuralNetwork.getLearningRule();
        learningRule.setMaxError(maxError);
        learningRule.setLearningRate(learningRate);
        learningRule.setMaxIterations(maxIterations);
        learningRule.addListener(new LearningEventListener() {
            @Override
            public void handleLearningEvent(LearningEvent learningEvent) {
                SupervisedLearning rule = (SupervisedLearning) learningEvent.getSource();
                System.out.println("Network error for iteration " + rule.getCurrentIteration() + ": " + rule.getTotalNetworkError());
            }
        });

        DataSet trainingSet = loadTrainingData(learningDataFilePath);
        neuralNetwork.learn(trainingSet);
        neuralNetwork.save(neuralNetworkModelFilePath);
    }

    DataSet loadTrainingData(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        DataSet trainingSet = new DataSet(slidingWindowSize, 1);

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");

                double trainValues[] = new double[slidingWindowSize];
                for (int i = 0; i < slidingWindowSize; i++) {
                    trainValues[i] = Double.valueOf(tokens[i]);
                }
                double expectedValue[] = new double[]{
                    Double.valueOf(tokens[slidingWindowSize])};
                trainingSet.addRow(new DataSetRow(trainValues, expectedValue));
            }
        } finally {
            reader.close();
        }
        return trainingSet;
    }

    double testNetwork() {
        NeuralNetwork neuralNetwork = NeuralNetwork.createFromFile(
                neuralNetworkModelFilePath);

        neuralNetwork.setInput(normalizeValue(2056.15),
                normalizeValue(2061.02), normalizeValue(2086.24),
                normalizeValue(2067.89), normalizeValue(2059.69));

        neuralNetwork.calculate();
        double[] networkOutput = neuralNetwork.getOutput();
        System.out.println("Expected value : 2066.96");
        System.out.println("Predicted value : " + deNormalizeValue(networkOutput[0]));

        return Math.sqrt(Math.pow(2066.96 - deNormalizeValue(networkOutput[0]), 2));
    }

    double dynamicTestNetwork() throws IOException {
        NeuralNetwork neuralNetwork = NeuralNetwork.createFromFile(
                neuralNetworkModelFilePath);

        prepareTestData(nNetworks);

        BufferedReader reader = new BufferedReader(new FileReader(testingDataFilePath));

        //reader.readLine();
        //reader.readLine();
        String line = reader.readLine();

        List<Double> aux = new ArrayList<>(slidingWindowSize);

        for (int i = nNetworks - 1; i > nNetworks - 1 - slidingWindowSize; i--) {
            //aux[Math.abs(i - nNetworks + 1)] = Double.valueOf(line.split(",")[i]);
            aux.add(Double.valueOf(line.split(",")[i]));
        }

        Collections.reverse(aux);

        double[] inputs = new double[slidingWindowSize];

        for (int i = 0; i < aux.size(); i++) {
            inputs[i] = aux.get(i);
        }

        neuralNetwork.setInput(inputs);

        neuralNetwork.calculate();
        double[] networkOutput = neuralNetwork.getOutput();
        double expectedValue = deNormalizeTestValue(Double.valueOf(line.split(",")[nNetworks]));
        System.out.println("Expected value : " + expectedValue);
        System.out.println("Predicted value : " + deNormalizeTestValue(networkOutput[0]));

        return Math.sqrt(Math.pow(expectedValue - deNormalizeTestValue(networkOutput[0]), 2));
    }
}
