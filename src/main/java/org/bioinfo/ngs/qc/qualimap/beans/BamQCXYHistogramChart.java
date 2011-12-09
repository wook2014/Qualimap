package org.bioinfo.ngs.qc.qualimap.beans;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

public class BamQCXYHistogramChart {
	// org.bioinfo.ntools.main params
	private String title;
	private String subTitle;
	private String xLabel;
	private String yLabel;
	
	// histogram series
	private List<String> names;
	private List<XYVector> histograms;
	private List<Color> colors;
	
	// chart
	private JFreeChart chart;
	
	private int numberOfBins;
	private boolean cumulative;
	private boolean zoomed;
	private double maxValue;
	private boolean rangeAxisIntegerTicks;
	private boolean domainAxisIntegerTicks;
	private boolean adjustDomainAxisLimits;
	
	public BamQCXYHistogramChart(String title,String subTitle, String xLabel, String yLabel){
		
		// main params
		this.title = title;
		this.subTitle = subTitle;
		this.xLabel = xLabel;
		this.yLabel = yLabel;
		
		// 
		names = new ArrayList<String>();
		histograms = new ArrayList<XYVector>();
		colors = new ArrayList<Color>();
		numberOfBins = 50;
		maxValue = 100;
		adjustDomainAxisLimits = true;
		
	}
	
	
	public void addHistogram(String name, XYVector histogram, Color color){
		names.add(name);
		histograms.add(histogram);
		colors.add(color);
	}
	
	
	public void render() throws IOException{
		
		// init chart
		chart = ChartFactory.createXYBarChart(title,xLabel,false,yLabel, null, PlotOrientation.VERTICAL, true, true, false);
		
		// title
		TextTitle textTitle = new TextTitle(title);
		textTitle.setFont(new Font(Font.SANS_SERIF,Font.BOLD,18));
		textTitle.setPaint(Color.darkGray);
		chart.setTitle(textTitle);		
		
		// subtitle
		TextTitle sub = new TextTitle(subTitle);
		Font subFont = new Font(Font.SANS_SERIF,Font.PLAIN,12);	
		sub.setFont(subFont);
		sub.setPadding(5, 5, 15, 5);
		sub.setPaint(Color.darkGray);
		chart.setSubtitles(Arrays.asList(sub));
						
		// other params
		chart.setPadding(new RectangleInsets(30,20,30,20));		
		
		XYPlot plot = chart.getXYPlot();
		// axis style
		Font axisFont = new Font(Font.SANS_SERIF,Font.PLAIN,11);
		plot.getDomainAxis().setLabelFont(axisFont);
		plot.getRangeAxis().setLabelFont(axisFont);
		Font tickFont = new Font(Font.SANS_SERIF,Font.PLAIN,10);
		plot.getDomainAxis().setTickLabelFont(tickFont);
		plot.getRangeAxis().setTickLabelFont(tickFont);
		if(domainAxisIntegerTicks){
			NumberAxis numberaxis = (NumberAxis)chart.getXYPlot().getDomainAxis();   
		    numberaxis.setAutoRangeIncludesZero(false);   
		    numberaxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		}
		
		if(rangeAxisIntegerTicks){
			NumberAxis numberaxis = (NumberAxis)chart.getXYPlot().getRangeAxis();   
		    numberaxis.setAutoRangeIncludesZero(false);
		    numberaxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		}

		// grid
		plot.setBackgroundPaint(Color.WHITE);
		chart.setBackgroundPaint(new Color(230,230,230));
		plot.setDomainGridlinePaint(new Color(214,139,74));
		plot.setRangeGridlinePaint(new Color(214,139,74));
		
		// prepare series		
		if(!zoomed && histograms.get(0).getSize() > 0) {
			maxValue = histograms.get(0).get(histograms.get(0).getSize()-1).getX();
		}
		
		double inc = maxValue/(double)numberOfBins;		
		double [] covs = new double[numberOfBins+1];	
		for(int i=0; i<covs.length; i++){
			covs[i] = inc*i;
		}
		
		// convert to bins
		double [] values = new double[numberOfBins+1];		
		double [] rfreqs = new double[numberOfBins+1];
		XYItem item;
		int pos;
		double total = 0;
		
		for(int i=0; i<histograms.get(0).getSize(); i++){
			item = histograms.get(0).get(i);
//			if(item.getX()>0){
				pos = (int)Math.floor(item.getX()/inc);
				if(pos<covs.length){
					values[pos] = values[pos] + item.getY();
					rfreqs[pos] = rfreqs[pos] + 1;					
				}
				total+=item.getY();
//			}
		}

	
		
		XYSeries series = new XYSeries("frequencies");
		double acum = 0;
		double next = 0;

		for(int i=0; i<values.length; i++){
			if(cumulative){
				acum += (values[i]/total)*100.0;
				next = acum;
			} else {
				next = values[i];
			}			
			series.add(covs[i],next);			
		}
				
		// mean dataset
		chart.getXYPlot().setDataset(0, new XYSeriesCollection(series));
		
		// mean renderer
		XYBarRenderer renderer = new XYBarRenderer();
		BamXYBarPainter barPainter = new BamXYBarPainter(numberOfBins);
		renderer.setBarPainter(barPainter);
		plot.setRenderer(renderer);

		// adjust axis limits
		if(adjustDomainAxisLimits && histograms.get(0).getSize() > 0){
            double minDomainAxis = histograms.get(0).get(0).getX() - inc/2.0;
		    double maxDomainAxis = maxValue + inc/2.0;
			chart.getXYPlot().getDomainAxis().setRange(minDomainAxis,maxDomainAxis); 
		}
	}


	public void zoom(double maxValue){
		this.maxValue = maxValue;
		zoomed = true;		
	}
	
	/**
	 * @return the chart
	 */
	public JFreeChart getChart() {
		return chart;
	}


	/**
	 * @param chart the chart to set
	 */
	public void setChart(JFreeChart chart) {
		this.chart = chart;
	}

	/**
	 * @return the numberOfBins
	 */
	public int getNumberOfBins() {
		return numberOfBins;
	}


	/**
	 * @param numberOfBins the numberOfBins to set
	 */
	public void setNumberOfBins(int numberOfBins) {
		this.numberOfBins = numberOfBins;
	}


	/**
	 * @return the zoomed
	 */
	public boolean isZoomed() {
		return zoomed;
	}


	/**
	 * @param zoomed the zoomed to set
	 */
	public void setZoomed(boolean zoomed) {
		this.zoomed = zoomed;
	}


	/**
	 * @return the maxValue
	 */
	public double getMaxValue() {
		return maxValue;
	}


	/**
	 * @param maxValue the maxValue to set
	 */
	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}


	/**
	 * @return the cumulative
	 */
	public boolean isCumulative() {
		return cumulative;
	}


	/**
	 * @param cumulative the cumulative to set
	 */
	public void setCumulative(boolean cumulative) {
		this.cumulative = cumulative;
	}


	/**
	 * @return the rangeAxisIntegerTicks
	 */
	public boolean isRangeAxisIntegerTicks() {
		return rangeAxisIntegerTicks;
	}


	/**
	 * @param rangeAxisIntegerTicks the rangeAxisIntegerTicks to set
	 */
	public void setRangeAxisIntegerTicks(boolean rangeAxisIntegerTicks) {
		this.rangeAxisIntegerTicks = rangeAxisIntegerTicks;
	}


	/**
	 * @return the domainAxisIntegerTicks
	 */
	public boolean isDomainAxisIntegerTicks() {
		return domainAxisIntegerTicks;
	}


	/**
	 * @param domainAxisIntegerTicks the domainAxisIntegerTicks to set
	 */
	public void setDomainAxisIntegerTicks(boolean domainAxisIntegerTicks) {
		this.domainAxisIntegerTicks = domainAxisIntegerTicks;
	}
}