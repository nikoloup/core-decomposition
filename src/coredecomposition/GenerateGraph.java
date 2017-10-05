/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package coredecomposition;

import coredecomposition.graphics.WindowUtilities;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

/**
 *
 * @author Diomedea Exulans
 */
public class GenerateGraph{
    
    private final long numberOfVertices;
    private long numberOfEdges;
    private final double connxProb;
    private final Path filePath;
    private final Random generator;
    private final JDialog frameProg;
    private final JDialog framePar;
    private final JProgressBar bar;
    
    private BufferedWriter writer;
    
    public GenerateGraph(long verts,double prob, Path file, JDialog frameProg, JDialog framePar, JProgressBar bar){
        this.numberOfVertices = verts;
        this.numberOfEdges = 0;
        this.connxProb = prob; //Scale to integers
        this.filePath = file;
        this.frameProg = frameProg;
        this.framePar = framePar;
        this.bar = bar;
        
        this.generator = new Random();
        
        checkInputs();
        init();
        
    }
    
    class Task extends SwingWorker<Void,Void>{
    
        @Override
        public Void doInBackground(){
            frameProg.setVisible(true);
            WindowUtilities.centreWindow(frameProg);
            startGraph();
            return null;
        }
        
        @Override
        public void done(){
            frameProg.setVisible(false);
            framePar.setVisible(false);
        }
    }
    
    private void init(){
        Task task = new Task();
        task.execute();
    }
    
    private boolean startGraph(){
        
        //Initialize reader
        initWriter();
        
        //Begin Graph Creation
        //For each node in the graph
        try{
            //Write Graph total size (vertices)
            writer.write(numberOfVertices+"");
            writer.newLine();
            double dice;
            for(int i=0; i<numberOfVertices; i++){
                //For every other node in the graph not already processed
                for(int j=i+1; j<numberOfVertices; j++){
                    //Roll a dice
                    dice = generator.nextDouble();
                    //If the throw is successful, generate an edge
                    if(dice<connxProb){
                        writer.write(i+","+j);
                        writer.newLine();
                    }
                }
                bar.setValue((int) Math.floor((((float)i)/numberOfVertices)*100));
            }
            //Close the file
            writer.close();
        } 
        catch(IOException ex){
            Logger.getLogger(GenerateGraph.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    private void initWriter(){
        
        Charset charset = Charset.forName("US-ASCII");
        try{
            this.writer = Files.newBufferedWriter(this.filePath, charset);
        } 
        catch (IOException ex) {
            Logger.getLogger(GenerateGraph.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        
    }

    private void checkInputs() {
        if(this.numberOfVertices<=1 ||
           this.connxProb <= 0 || this.connxProb >= 100)
        throw new IllegalArgumentException("Invalid input "+this.numberOfVertices+" "+this.connxProb);
    }
}
