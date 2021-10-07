package com.ggl.testing;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class ReactionTestingGUI implements Runnable {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new ReactionTestingGUI());
	}
	
	private final DrawingPanel drawingPanel;
	
	private JScrollPane scrollPane;
	
	private final ReactionTableModel tableModel;
	
	private final ReactionTestingModel model;
	
	public ReactionTestingGUI() {
		this.model = new ReactionTestingModel();
		this.tableModel = new ReactionTableModel();
		this.drawingPanel = new DrawingPanel(this, model);
	}

	@Override
	public void run() {
		JFrame frame = new JFrame("Reaction Testing");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.add(createTablePanel(), BorderLayout.WEST);
		frame.add(drawingPanel, BorderLayout.CENTER);
		
		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
		
		System.out.println("Frame size: " + frame.getSize());
	}
	
	private JPanel createTablePanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		String[] columns = { "Expected Time", "Actual Time", "Difference" };
		for (String columnName : columns) {
			tableModel.addColumn(columnName);
		}
		
		JTable table = new JTable(tableModel);
		DecimalFormatRenderer dfr = new DecimalFormatRenderer();
		table.getColumnModel().getColumn(0).setCellRenderer(dfr);
		table.getColumnModel().getColumn(1).setCellRenderer(dfr);
		table.getColumnModel().getColumn(2).setCellRenderer(dfr);
		scrollPane = new JScrollPane(table);
		panel.add(scrollPane, BorderLayout.CENTER);
		
		return panel;
	}
	
	public void addResponse(Reaction reaction) {
		Object[] rowData = new Object[3];
		rowData[0] = reaction.getExpectedTime();
		rowData[1] = reaction.getActualTime();
		rowData[2] = reaction.getAbsoluteDifference();
		tableModel.insertRow(0, rowData);
		scrollPane.getViewport().setViewPosition(new Point(0,0));
	}
	
	public void repaint() {
		drawingPanel.repaint();
	}
	
	public class DrawingPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		
		private final int margin, drawingHeight, drawingWidth;
		
		private final ReactionTestingModel model;
		
		public DrawingPanel(ReactionTestingGUI view, ReactionTestingModel model) {
			this.model = model;
			
			this.margin = 40;
			this.drawingHeight = 200;
			this.drawingWidth = 400;
			
			int width = drawingWidth + margin + margin;
			int height = drawingHeight + margin + margin;
			this.setBackground(Color.WHITE);
			this.setPreferredSize(new Dimension(width, height));
			this.addMouseListener(new PanelListener(view, model));
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
					RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, 
					RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			
			int rectangleHeight = paintRectangle(g2d);
			
			double expectedTime = model.getCurrentReaction().getExpectedTime();
			if (expectedTime > 0.0) {
				int textDisplacement = 30;
				int x = margin;
				int y = margin + rectangleHeight + textDisplacement;
				paintExpectedTime(g2d, x, y, expectedTime);
				
				double actualTime = model.getCurrentReaction().getActualTime();
				if (actualTime > 0.0) {
					y = paintActualTime(g2d, x, y, textDisplacement, actualTime);
					y = paintDifference(g2d, x, y, textDisplacement);
					paintBar(g2d, actualTime, rectangleHeight);
				}
				
				paintStopLine(g2d, expectedTime, rectangleHeight);
			}
		}

		private int paintRectangle(Graphics2D g2d) {
			int x = margin;
			int y = margin;
			int rectangleHeight = 40;
			g2d.setStroke(new BasicStroke(3f));
			g2d.setColor(Color.BLACK);
			g2d.drawRect(x, y, drawingWidth, rectangleHeight);
			return rectangleHeight;
		}

		private void paintExpectedTime(Graphics2D g2d, int x, int y, double expectedTime) {
			String expectedTimeString = "Expected time: " + String.format("%5.2f", expectedTime);
			g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
			g2d.setColor(Color.BLACK);
			g2d.drawString(expectedTimeString, x, y);
		}

		private int paintActualTime(Graphics2D g2d, int x, int y, int textDisplacement, 
				double actualTime) {
			y += textDisplacement;
			String actualTimeString = "Actual time:   " + String.format("%5.2f", actualTime);
			g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
			g2d.setColor(Color.BLACK);
			g2d.drawString(actualTimeString, x, y);
			return y;
		}

		private int paintDifference(Graphics2D g2d, int x, int y, int textDisplacement) {
			y += textDisplacement;
			double difference = model.getCurrentReaction().getAbsoluteDifference();
			String differenceTimeString = "Difference:    " + String.format("%5.2f", difference);
			g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
			g2d.setColor(Color.BLACK);
			g2d.drawString(differenceTimeString, x, y);
			return y;
		}

		private void paintBar(Graphics2D g2d, double actualTime, int rectangleHeight) {
			int x = margin + 1;
			int y = margin + 1;
			int actualWidth = (int) Math.round(actualTime * 10);
			g2d.setColor(Color.GREEN);
			g2d.fillRect(x, y, actualWidth, rectangleHeight - 2);
		}

		private void paintStopLine(Graphics2D g2d, double expectedTime, int rectangleHeight) {
			int x = (int) Math.round(expectedTime * 10.0) + margin;
			int y1 = margin;
			int y2 = margin + rectangleHeight;
			float[] dash1 = { 2f, 0f, 2f };
			BasicStroke bs1 = new BasicStroke(3f, BasicStroke.CAP_BUTT, 
					BasicStroke.JOIN_ROUND, 1.0f, dash1, 2f);
			g2d.setStroke(bs1);
			g2d.setColor(Color.RED);
			g2d.drawLine(x, y1, x, y2);
		}
		
	}
	
	public class DecimalFormatRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;
		
		private final JLabel cellLabel = new JLabel();

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, 
				boolean isSelected, boolean hasFocus, int row, int column) { 
			cellLabel.setHorizontalAlignment(JLabel.TRAILING);
			cellLabel.setText(String.format("%5.2f", (Number) value));
			return cellLabel;
		}
	}
	
	public class PanelListener extends MouseAdapter {
		
		private final ReactionTestingGUI view;
		
		private final ReactionTestingModel model;
		
		private final Timer timer;

		public PanelListener(ReactionTestingGUI view, ReactionTestingModel model) {
			this.view = view;
			this.model = model;
			this.timer = new Timer(40, new TimerListener(view, model));
		}

		@Override
		public void mouseReleased(MouseEvent event) {
			if (event.getButton() == MouseEvent.BUTTON1) {
				if (model.isRunning()) {
					timer.stop();
					model.addReaction(model.getCurrentReaction());
					view.addResponse(model.getCurrentReaction());
					model.setRunning(false);
				} else {
					Reaction currentReaction = new Reaction(generateRandomDouble());
					model.setCurrentReaction(currentReaction);
					timer.restart();
					model.setRunning(true);
				}
			}
		}
		
		private double generateRandomDouble() {
			double x1 = 10.0;
			double x2 = 30.0;
			double f = Math.random() / Math.nextDown(1.0);
			return x1 * (1.0 - f) + x2 * f;
		}
		
	}
	
	public class TimerListener implements ActionListener {
		
		private final ReactionTestingGUI view;
		
		private final ReactionTestingModel model;

		public TimerListener(ReactionTestingGUI view, ReactionTestingModel model) {
			this.view = view;
			this.model = model;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			Timer timer = (Timer) event.getSource();
			int delay = timer.getDelay();
			double increment = 0.001 * delay;
			model.getCurrentReaction().incrementActualTime(increment);
			view.repaint();
		}
		
	}
	
	public class ReactionTableModel extends DefaultTableModel {

		private static final long serialVersionUID = 1L;

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}
		
	}
	
	public class ReactionTestingModel {
		
		private boolean running;
		
		private final List<Reaction> reactions;
		
		private Reaction currentReaction;
		
		public ReactionTestingModel() {
			this.reactions = new ArrayList<>();
			this.currentReaction = new Reaction(0.0);
			this.running = false;
		}
		
		public void addReaction(Reaction reaction) {
			this.reactions.add(reaction);
		}

		public List<Reaction> getReactions() {
			return reactions;
		}
		
		public Reaction getCurrentReaction() {
			return currentReaction;
		}

		public void setCurrentReaction(Reaction currentReaction) {
			this.currentReaction = currentReaction;
		}

		public boolean isRunning() {
			return running;
		}

		public void setRunning(boolean running) {
			this.running = running;
		}
		
	}
	
	public class Reaction {
		
		private double actualTime;
		
		private final double expectedTime;

		public Reaction(double expectedTime) {
			this.expectedTime = expectedTime;
			this.actualTime = 0.0;
		}

		public double getExpectedTime() {
			return expectedTime;
		}

		public double getActualTime() {
			return actualTime;
		}
		
		public void setActualTime(double actualTime) {
			this.actualTime = actualTime;
		}
		
		public void incrementActualTime(double increment) {
			this.actualTime += increment;
		}

		public double getAbsoluteDifference() {
			return Math.abs(expectedTime - actualTime);
		}
		
	}

}
