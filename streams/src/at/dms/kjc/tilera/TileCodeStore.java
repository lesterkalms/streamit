package at.dms.kjc.tilera;

import at.dms.kjc.backendSupport.ComputeCodeStore;
import at.dms.kjc.common.ALocalVariable;
import at.dms.kjc.*;

public class TileCodeStore extends ComputeCodeStore<Tile> {
    /** True if this tile code store has code appended to it */
    private boolean hasCode = false;
    /** The method that will malloc the buffers and receive addresses from downstream
     * tiles*/
    protected JMethodDeclaration bufferInit;
    /** The name of the bufferInit method */
    public static final String bufferInitMethName = "buffer_and_address_init";
    
    public TileCodeStore(Tile nodeType) {
        super(nodeType);
        setMyMainName("__main__");
        createBufferInitMethod();
    }
    
    /**
     * Constructor: caller will add code to bound number of iterations, code store has pointer back to a compute node.
     * @param parent a ComputeNode.
     * @param iterationBound a variable that will be defined locally in 
     */
    public TileCodeStore(Tile parent, ALocalVariable iterationBound) {
       super(parent, iterationBound);
       setMyMainName("__main__");
       createBufferInitMethod();
    }
    
    private void createBufferInitMethod() {
        //create the method that will malloc the buffers and receive the addresses from downstream tiles
        bufferInit = new JMethodDeclaration(CStdType.Void, bufferInitMethName, new JFormalParameter[0], new JBlock());
        addMethod(bufferInit);
    }
    
    /**
     * Add stmt to the end of the method that will perform the allocation of buffers
     * and receive addresses of buffers from downstream tiles.
     * 
     * @param stmt The statement to add to the end of the method
     */
    public void addStatementToBufferInit(JStatement stmt) {
        bufferInit.getBody().addStatement(stmt);
    }
    
    /**
     * Constructor: caller will add code to bound number of iterations, no pointer back to compute node.
     * @param iterationBound a variable that will be defined locally by <code>getMainFunction().addAllStatments(0,stmts);</code>
     */
    public TileCodeStore(ALocalVariable iterationBound) {
        super(iterationBound);
        setMyMainName("__main__");
    }
    
    /**
     * Constructor: steady state loops indefinitely, no pointer back to compute node.
     */
    public TileCodeStore() {
        super();
        setMyMainName("__main__");
    }
    
    /**
     * Return true if we should generate code for this tile,
     * false if no code was ever generated for this tile.
     * 
     * @return true if we should generate code for this tile,
     * false if no code was ever generated for this tile.
     */
    public boolean shouldGenerateCode() {
        return hasCode;
    }
    
    /** 
     * Set that this tile (code store) has code written to it and thus 
     * it needs to be considered during code generation.
     */
    public void setHasCode() {
        hasCode = true;
    }
}
