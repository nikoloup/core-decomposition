/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package coredecomposition;

import coredecomposition.graphics.WindowUtilities;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

/**
 *
 * @author Diomedea Exulans
 */
public class CoreDecomposition {
    
    private final int maxMemory;
    private final Path filePath;
    private final JDialog dia;
    private final JProgressBar bar;
    private final JTextArea textarea;
    
    private BufferedReader reader;
    private int maxEdges;
    
    public CoreDecomposition(int mem, Path file, JDialog dia, JProgressBar bar, JTextArea text){
        this.maxMemory = mem;
        this.filePath = file;
        
        this.dia = dia;
        this.bar = bar;
        this.textarea = text;
        
        checkInputs();
        init();
    }
    
    private class Task extends SwingWorker<Void,Void>{
        @Override
        public Void doInBackground(){
            dia.setVisible(true);
            WindowUtilities.centreWindow(dia);
            bar.setValue(0);
            coreDecomp();
            return null;
        }
        
        @Override
        public void done(){
            dia.setVisible(false);
        }
        
    }
    
    private void coreDecomp(){
        
        String buffer;
        int nrVertices;
        int nrEdges = 0;
        int[] vertices; //Contains degrees of vertices, where array key is node id
        int[] ordered; //Contains vertex ordering, where array value is node id and array key is order
        int[] binsStartPos; //Contains starting position of sorting bins. Helps in array reordering
        int maxVertDeg = 0; //Maximum vertex degree
        String[] bins;
        int srcV; //Edge source vertex
        int tarV; //Edge target vertex
        String[] edge;
        int count = 0;
        String[] binContent;
        int nrPasses = 0; //Counts number of passes
        int computedCoresPos = 0; //Specifies position of last computed core
        boolean decompositionComplete = false; //Process ending flag
        
        //Get number of vertices
        buffer = this.readLine();
        nrVertices = Integer.parseInt(buffer);

        if(nrVertices==-1){
            throw new IllegalArgumentException("Input graph incompatible/corrupt"); 
        }

        //Build array of all vertices
        vertices = new int[nrVertices];
        ordered = new int[nrVertices];
        for(int i=0; i<nrVertices; i++){
            vertices[i] = 0;
        }
        
        //Compute degrees of all vertices
        buffer = this.readLine();
        while(buffer!=null && !buffer.equals("-1")){
            edge = buffer.split(",");
            srcV = Integer.parseInt(edge[0]);
            tarV = Integer.parseInt(edge[1]);
            vertices[srcV]++;
            vertices[tarV]++;
            if(vertices[srcV]>maxVertDeg){
                maxVertDeg = vertices[srcV];
            }
            if(vertices[tarV]>maxVertDeg){
                maxVertDeg = vertices[tarV];
            }
            nrEdges++;
            buffer = this.readLine();
        }
        
        //BINSORT
        //Initiate bins
        bins = new String[maxVertDeg+1];
        binsStartPos = new int[maxVertDeg+1];
        for(int i=0; i<=maxVertDeg; i++){
            bins[i] = "";
        }
        //Allocate vertices in bins
        for(int i=0; i<nrVertices; i++){
            bins[vertices[i]] = bins[vertices[i]] + "," + i;
        }
        for(int i=0; i<=maxVertDeg; i++){
            if(!bins[i].equals("")) bins[i] = bins[i].substring(1);
        }
        //Place ordered vertices in new array
        for(int i=0; i<=maxVertDeg; i++){
            binContent = bins[i].split(",");
            //Mark bin start position
            binsStartPos[i] = count;
            for(String binContent1 : binContent){
                if(!binContent1.equals("")){
                    ordered[count] = Integer.parseInt(binContent1);
                    count++;
                }
            }
        }
        
        //Calculate max number of edges we can store in memory
        calcParams(nrVertices,nrEdges);
        
        logOutput("Graph has "+nrVertices+" vertices");
        logOutput("Graph has "+nrEdges+" edges");
        logOutput("Beginning Core Decomposition...");
        
        //CORE DECOMPOSITION
        
        while(!decompositionComplete){
            
            //Initialization Stage
            nrPasses++;
        
            //For each vertex in the vertices array, we need to load all its incident edges.
            //First, we need to calculate how many vertices' edges we can store in memory
            long degSum = 0;
            int[] thisRoundVerts = new int[nrVertices];
            int[][] thisRoundEdges = new int[this.maxEdges][2];
            int thisRoundVertsPos = 0; //This will double as our number of verts for this round
            int thisRoundEdgesPos = 0;
            int orderedPos = computedCoresPos; //Start from next core to compute
            while(degSum<this.maxEdges){
                if(vertices[ordered[orderedPos]]+degSum<this.maxEdges){
                    degSum += vertices[ordered[orderedPos]];
                    thisRoundVerts[thisRoundVertsPos] = ordered[orderedPos];
                    thisRoundVertsPos++;
                    orderedPos++;
                    if(orderedPos>nrVertices-1){
                        //Last vertex reached
                        break;
                    }
                }
                else{
                    //Maximum storage capacity reached
                    break;
                }
            }

            //Now we have all the vertexes whose core numbers we are going to compute.
            //As we will have to iteratively search if an edge is of interest to us or not,
            //we sort this array to make things quicker
            Arrays.sort(thisRoundVerts, 0, thisRoundVertsPos); //End range argument is exclusive :)

            //Reset the reader to the beginning of file (create a new one)
            initReader();

            //Read all edges and keep in memory only those of interest to us
            buffer = this.readLine(); //Skip number of edges line
            buffer = this.readLine();
            edge_read_loop: //Labels edge-reading loop for continue statement
            while(buffer!=null && !buffer.equals("-1")){
                edge = buffer.split(",");
                srcV = Integer.parseInt(edge[0]);
                tarV = Integer.parseInt(edge[1]);
                //Check if core is already computed for edge source or target and if yes skip edge
                for(int i=0; i<computedCoresPos; i++){
                    if(ordered[i]==srcV || ordered[i]==tarV){
                        buffer = this.readLine();
                        continue edge_read_loop;
                    }
                }
                //Check if edge is relevant to this round's vertices
                for(int i=0; i<thisRoundVertsPos; i++){
                    if(thisRoundVerts[i]==srcV || thisRoundVerts[i]==tarV){ 
                        //Keep edge
                        thisRoundEdges[thisRoundEdgesPos][0] = srcV;
                        thisRoundEdges[thisRoundEdgesPos][1] = tarV;
                        thisRoundEdgesPos++;
                        break;
                    }
                }
                buffer = this.readLine();
            }

            //Decomposition Stage

            //We now begin the calculation of core numbers for the vertices of this round
            int currentVert;
            while(computedCoresPos<nrVertices){
                //Get the next vertex to compute
                currentVert = ordered[computedCoresPos];
                //If current vertex is not in this round's vertices, we don't
                //have its edges in memory (due to a vertex degree reorder from a core computation). 
                //In this case we must scrap all data in memory and begin a new round.
                boolean flagFound = false;
                for(int i=0; i<thisRoundVertsPos; i++){
                    if(thisRoundVerts[i]==currentVert){
                        flagFound = true;
                        break;
                    }
                }
                if(!flagFound){
                    break;
                }
                //Go through this round's edges.
                //For each matching edge, lower the neighboring vertex's degree by one
                //TODO: We can limit the search by also checking against currentVert degree
                for(int i=0; i<thisRoundEdgesPos; i++){
                    if(thisRoundEdges[i][0] == currentVert){
                        //Lower neighbor degree if higher than current vert's
                        if(vertices[currentVert]<vertices[thisRoundEdges[i][1]]){
                            vertices[thisRoundEdges[i][1]]--;
                        }
                        else{
                            continue;
                        }
                        //Reorder vertices
                        //A vertex could only move to the previous bin. As such, a simple
                        //swap with the previous bin start and an increase of the current bin
                        //start will suffice
                        int smallBin = binsStartPos[vertices[thisRoundEdges[i][1]]];
                        int bigBin = binsStartPos[vertices[thisRoundEdges[i][1]]+1];
                        int vertToMove = ordered[smallBin];
                        int vertToMove2 = ordered[bigBin];
                        ordered[smallBin] = thisRoundEdges[i][1];
                        ordered[bigBin] = vertToMove;
                        for(int j=bigBin; j<nrVertices; j++){
                            if(ordered[j]==thisRoundEdges[i][1]){
                                ordered[j] = vertToMove2;
                                break;
                            }
                        }
                        binsStartPos[vertices[thisRoundEdges[i][1]]+1]++;

                    }
                    else if(thisRoundEdges[i][1] == currentVert){
                        //Lower neighbor degree if higher than current vert's
                        if(vertices[currentVert]<vertices[thisRoundEdges[i][0]]){
                            vertices[thisRoundEdges[i][0]]--;
                        }
                        else{
                            continue;
                        }
                        //Reorder vertices
                        //A vertex could only move to the previous bin. As such, a simple
                        //swap with the previous bin start and an increase of the current bin
                        //start will suffice
                        int smallBin = binsStartPos[vertices[thisRoundEdges[i][0]]];
                        int bigBin = binsStartPos[vertices[thisRoundEdges[i][0]]+1];
                        int vertToMove = ordered[smallBin];
                        int vertToMove2 = ordered[bigBin];
                        ordered[smallBin] = thisRoundEdges[i][0];
                        ordered[bigBin] = vertToMove;
                        for(int j=bigBin; j<nrVertices; j++){
                            if(ordered[j]==thisRoundEdges[i][0]){
                                ordered[j] = vertToMove2;
                                break;
                            }
                        }
                        binsStartPos[vertices[thisRoundEdges[i][0]]+1]++;
                    }
                }
                //Calculate core of next vertex
                computedCoresPos++;
                bar.setValue((int)Math.floor(((float)computedCoresPos/nrVertices)*100));
            }
            
            if(computedCoresPos == nrVertices){
                decompositionComplete = true;
            }
        }
        
        //Write results
        logOutput("Core Decomposition Completed.");
        logOutput(nrPasses+" passes performed.");
        String out = "";
        int curr = vertices[ordered[0]];
        int core = -1;
        for(int i=1; i<nrVertices; i++){
            if(curr!=core){
                if(core!=-1){
                    if(out.charAt(out.length()-1)==','){
                        out = out.substring(0,out.length()-1);
                    }
                    logOutput(out);
                }
                core = curr;
                out = "Core "+core+":";
            }
            out = out + ordered[i] + ",";
            curr = vertices[ordered[i]];
        }
        if(out.charAt(out.length()-1)==','){
            out = out.substring(0,out.length()-1);
        }
        logOutput(out);
        
        //System.out.println(computedCoresPos);
        //Let's see the results!
        //for(int i=0; i<computedCoresPos; i++){
        //    System.out.println("Node "+ordered[i]+", "+vertices[ordered[i]]);
        //}
        //System.out.println("Completed");
        
    }
    
    private void calcParams(int verts, int edges){
        if(this.maxMemory < verts){
            this.maxEdges = verts;
            logOutput("Memory parameter overriden, set to "+this.maxEdges);
        }
        else{
            this.maxEdges = this.maxMemory;
        }
    }
    
    private void logOutput(String text){
        this.textarea.append(text+"\n");
    }
    
    private void updateProgress(int curr, int total){
        float per = 1;
        //bar.setValue((int)Math.floor((curr/total)*100));
    }
    
    private String readLine(){
        try{
            return this.reader.readLine();
        }
        catch(Exception e){
            System.out.println("Couldn't read from file");
            System.out.println(e);
        }
        return "-1";
    }
    
    private void initReader(){
        
        Charset charset = Charset.forName("US-ASCII");
        try{
            this.reader = Files.newBufferedReader(this.filePath, charset);
        } 
        catch (IOException ex) {
            Logger.getLogger(CoreDecomposition.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        
    }
    
    private void checkInputs(){
        if(maxMemory==0){
            throw new IllegalArgumentException("Options cannot be empty or zero");
        }
    }
    
    private void init(){
        initReader();
        Task task = new Task();
        task.execute();
    }
}
