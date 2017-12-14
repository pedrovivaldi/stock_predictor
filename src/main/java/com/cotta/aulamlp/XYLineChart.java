/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cotta.aulamlp;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;

/**
 *
 * @author PedroHenrique
 */
public class XYLineChart extends JFrame {
    
    private XYDataset dataset;
    
    public XYLineChart() {
        super();
        
        setSize(640, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
    }
    
    public void show(String title) {
        JPanel chartPanel = createChartPanel(title);
        add(chartPanel, BorderLayout.CENTER);
        setVisible(true);
    }
    
    private JPanel createChartPanel(String title) {
        String chartTitle = title;
        String xAxisLabel = "X";
        String yAxisLabel = "Y";
        
        JFreeChart chart = ChartFactory.createXYLineChart(chartTitle, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, rootPaneCheckingEnabled, rootPaneCheckingEnabled, rootPaneCheckingEnabled);
        
        return new ChartPanel(chart);
    }
    
    public void setDataset(XYDataset dataset) {
        this.dataset = dataset;
    }
    
}
