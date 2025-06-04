
import java.util.HashMap;
import java.util.Map;

import IR.token.Identifier;
import sparrowv.Add;
import sparrowv.Alloc;
import sparrowv.Call;
import sparrowv.ErrorMessage;
import sparrowv.FunctionDecl;
import sparrowv.Goto;
import sparrowv.IfGoto;
import sparrowv.LabelInstr;
import sparrowv.LessThan;
import sparrowv.Load;
import sparrowv.Move_Id_Reg;
import sparrowv.Move_Reg_FuncName;
import sparrowv.Move_Reg_Id;
import sparrowv.Move_Reg_Integer;
import sparrowv.Move_Reg_Reg;
import sparrowv.Multiply;
import sparrowv.Print;
import sparrowv.Program;
import sparrowv.Store;
import sparrowv.Subtract;
import sparrowv.visitor.DepthFirst;

public class VTranslator extends DepthFirst {

    public StringBuilder riscProgram;              // RISC-V program
    private boolean isMain;
    private final Map<String, FunctionStruct> fmd;
    private String currentFunction;
    private FunctionStruct currFuncMetadata;
    private int count;

    public VTranslator(Map<String, FunctionStruct> fmd) {
        this.riscProgram = new StringBuilder();
        this.fmd = new HashMap<>(fmd);
    }

    /* Sparrow-V AST components */

    /*   List<FunctionDecl> funDecls; */
    @Override
    public void visit(Program n) {
        /* Add program header */ 
        riscProgram.append(".equiv @sbrk, 9\n");                                        // .equiv @sbrk, 9
        riscProgram.append(".equiv @print_string, 4\n");                                // .equiv @print_string, 4
        riscProgram.append(".equiv @print_char, 11\n");                                 // .equiv @print_char, 11
        riscProgram.append(".equiv @print_int, 1\n");                                   // .equiv @print_int, 1
        riscProgram.append(".equiv @exit, 10\n");                                       // .equiv @exit, 10
        riscProgram.append(".equiv @exit2, 17\n");                                      // .equiv @exit2, 17
        riscProgram.append("\n");

        riscProgram.append(".text\n");                                                  // .text
        riscProgram.append("\n");

        /* RISC-V entry point */
        riscProgram.append("\n.globl main\n");                                          // .globl main
        riscProgram.append("  jal Main\n");                                             // jal Main
        riscProgram.append("  li a0, @exit\n");                                         // li a0, @exit
        riscProgram.append("  ecall\n");                                                // ecall
        riscProgram.append("\n");
        
        /* Translate functions to RISC-V */
        int idx = 0;
        for (FunctionDecl fd : n.funDecls) {
            isMain = (idx == 0);
            fd.accept(this);
            idx++;
        }

        /* Print */
        riscProgram.append("\n.globl print\n");                                         // .globl print
        riscProgram.append("print:\n");                                                 // print:
        riscProgram.append("  mv a1, a0\n");                                            // mv a1, a0
        riscProgram.append("  li a0, @print_int\n");                                    // li a0, @print_int
        riscProgram.append("  ecall\n");                                                // ecall
        riscProgram.append("  li a1, 10\n");                                            // li a1, 10
        riscProgram.append("  li a0, @print_char\n");                                   // li a0, @print_char
        riscProgram.append("  ecall\n");                                                // ecall
        riscProgram.append("  jr ra\n");                                                // jr ra
        riscProgram.append("\n");

        /* Error */
        riscProgram.append("\n.globl error\n");                                         // .globl error
        riscProgram.append("error:\n");                                                 // error:
        riscProgram.append("  mv a1, a0\n");                                            // mv a1, a0
        riscProgram.append("  li a0, @print_string\n");                                 // li a0, @print_string
        riscProgram.append("  ecall\n");                                                // ecall
        riscProgram.append("  li a1, 10\n");                                            // li a1, 10
        riscProgram.append("  li a0, @print_char\n");                                   // li a0, @print_char
        riscProgram.append("  ecall\n");                                                // ecall
        riscProgram.append("  li a0, @exit\n");                                         // li a0, @exit
        riscProgram.append("  ecall\n");                                                // ecall
        riscProgram.append("abort_17:\n");                                              // abort_17:
        riscProgram.append("   j abort_17\n");                                          // j abort_17
        riscProgram.append("\n");

        /* Alloc */
        riscProgram.append("\n.globl alloc\n");                                         // .globl alloc
        riscProgram.append("alloc:\n");                                                 // alloc:
        riscProgram.append("  mv a1, a0\n");                                            // mv a1, a0
        riscProgram.append("  li a0, @sbrk\n");                                         // li a0, @sbrk
        riscProgram.append("  ecall\n");                                                // ecall
        riscProgram.append("  jr ra\n");                                                // jr ra
        riscProgram.append("\n");
        
        riscProgram.append(".data\n");                                                  // .data
        riscProgram.append("\n");

        /* Null pointer */
        riscProgram.append("\n.globl msg_nullptr\n");                                   // .globl msg_nullptr
        riscProgram.append("msg_nullptr:\n");                                           // msg_nullptr
        riscProgram.append("  .asciiz \"null pointer\"\n");                             // .asciiz "null pointer"
        riscProgram.append("  .align 2\n");                                             // .align 2
        riscProgram.append("\n");

        /* Array index out of bounds */
        riscProgram.append("\n.globl msg_array_oob\n");                                 // .globl msg_array_oob
        riscProgram.append("msg_array_oob:\n");                                         // msg_array_oob
        riscProgram.append("  .asciiz \"array index out of bounds\"\n");                // .asciiz "array index out of bounds"
        riscProgram.append("  .align 2\n");                                             // .align 2
    }

    /*   Program parent;
     *   FunctionName functionName;
     *   List<Identifier> formalParameters;
     *   Block block; */
    @Override
    public void visit(FunctionDecl n) {
        // TODO: Adjust sp and store fp
        currentFunction = n.functionName.toString();
        currFuncMetadata = fmd.get(currentFunction);

        if (isMain) {
            /* Sparrow-V Main function */
            riscProgram.append("\n.globl Main\n");                                      // .globl Main
            riscProgram.append("Main:\n");                                              // Main:
        } else {
            /* Other Sparrow-V function */
            String firstInstr = "\n.globl " + currentFunction + "\n";
            String secondInstr = currentFunction + ":\n";

            /* Build RISC-V instructions */
            riscProgram.append(firstInstr);                                                 // .globl Foo
            riscProgram.append(secondInstr);                                                // Foo:
        }

        /* Create new activation record */
        /* TODO: Fix this. Check first pass and stack offsets. Frame size looks too large for FacComputeFac (38 instead of 32)*/
        int frameSize = currFuncMetadata.getFrameSize();
        String storeFrameSize = "  li t6, " + frameSize + "\n";

        riscProgram.append("  sw fp, -8(sp)\n");
        riscProgram.append("  mv fp, sp\n");
        riscProgram.append(storeFrameSize);
        riscProgram.append("  sub sp, sp, t6\n");           // Question: CAN I DEFAULT TO T6 REGISTER?? DON'T SEE WHY NOT
        riscProgram.append("  sw ra, -4(fp)\n");

        /* TODO: Fix this. Double check if needed in the output since example does not have it. */
        /* Load local variables and arguments to new stack activation record */
        // Map<String, Integer> varOffsets = funcVarOffsets.get(currentFunction);

        // for (Map.Entry<String, Integer> entry : varOffsets.entrySet()) {
        //     String var = entry.getKey();
        //     int offset = entry.getValue();

        //     String instr = "  sw " + var + ", " + offset + "(fp)\n";                        // sw v0, -4(fp)
        //     riscProgram.append(instr);                                                      // sw arg1, 4(fp)
        // }

        /* Process instruction in function block */
        n.block.accept(this);

        /* TODO: Handle returns using a0 register */

        /* Store return value */
        String retId = n.block.return_id.toString();
        int offset = currFuncMetadata.hasVar(retId) ? currFuncMetadata.getVarOffsets().get(retId) : currFuncMetadata.getArgOffsets().get(retId);

        String storeRetId = "  lw a0, " + offset + "(fp)\n";
        riscProgram.append(storeRetId);

        /* Deallocate local variables and arguments from activation record */
        String restoreSp = "  addi sp, sp, " + frameSize + "\n";

        riscProgram.append("  lw ra, -4(fp)\n");
        riscProgram.append("  lw fp, -8(fp)\n");
        riscProgram.append(restoreSp);
        riscProgram.append("  jr ra\n");
    }

    /*   Label label; */
    @Override
    public void visit(LabelInstr n) {
        /* Deconstruct Sparrow-V instruction components */
        String label = n.label.toString();

        String mangled = currentFunction + "_" + label;

        /* Build RISC-V instruction */
        String instr = mangled + ":\n";
        riscProgram.append(instr);
    }

    /*   Register lhs;
     *   int rhs; */
    @Override
    public void visit(Move_Reg_Integer n) {
        /* Deconstruct Sparrow-V instruction components */
        String lhs = n.lhs.toString();
        int rhs = n.rhs;

        /* Build RISC-V instruction */
        String instr = "  li " + lhs + ", " + rhs + "\n";                                   // li t0, 1
        riscProgram.append(instr);
    }

    /*   Register lhs;
     *   FunctionName rhs; */
    @Override
    public void visit(Move_Reg_FuncName n) {
        /* Deconstruct Sparrow-V instruction components */
        String lhs = n.lhs.toString();
        String rhs = n.rhs.toString();

        /* Build RISC-V instruction */
        String instr = "  la " + lhs + ", " + rhs + "\n";                                   // la t0, Foo
        riscProgram.append(instr);
    }

    /*   Register lhs;
     *   Register arg1;
     *   Register arg2; */
    @Override
    public void visit(Add n) {
        /* Deconstruct Sparrow-V instruction components */
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();

        /* Build RISC-V instruction */
        String instr = "  add " + lhs + ", " + arg1 + ", " + arg2 + "\n";                   // add t0, t1, t2

        riscProgram.append(instr);
    }

    /*   Register lhs;
     *   Register arg1;
     *   Register arg2; */
    @Override
    public void visit(Subtract n) {
        /* Deconstruct Sparrow-V instruction components */
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();

        /* Build RISC-V instruction */
        String instr = "  sub " + lhs + ", " + arg1 + ", " + arg2 + "\n";                   // sub t0, t1, t2

        riscProgram.append(instr);
    }

    /*   Register lhs;
     *   Register arg1;
     *   Register arg2; */
    @Override
    public void visit(Multiply n) {
        /* Deconstruct Sparrow-V instruction components */
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();

        /* Build RISC-V instruction */
        String instr = "  mul " + lhs + ", " + arg1 + ", " + arg2 + "\n";                   // mul t0, t1, t2

        riscProgram.append(instr);
    }

    /*   Register lhs;
     *   Register arg1;
     *   Register arg2; */
    @Override
    public void visit(LessThan n) {
        /* Deconstruct Sparrow-V instruction components */
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();

        /* Build RISC-V instruction */
        String instr = "  slt " + lhs + ", " + arg1 + ", " + arg2 + "\n";                   // slt t0, t1, t2

        riscProgram.append(instr);
    }

    /*   Register lhs;
     *   Register base;
     *   int offset; */
    @Override
    public void visit(Load n) {
        /* Deconstruct Sparrow-V instuction components */
        String lhs = n.lhs.toString();
        String base = n.base.toString();
        int offset = n.offset;

        /* Build RISC-V instruction */
        String instr = "  lw " + lhs + ", " + offset + "(" + base + ")" + "\n";             // lw t0, 4(t1)

        riscProgram.append(instr);
    }

    /*   Register base;
     *   int offset;
     *   Register rhs; */
    @Override
    public void visit(Store n) {
        /* Deconstruct Sparrow-V instruction components */
        String base = n.base.toString();
        int offset = n.offset;
        String rhs = n.rhs.toString();

        /* Build RISC-V instruction */
        String instr = "  sw " + rhs + ", " + offset + "(" + base + ")" + "\n";             // sw t1, 4(t0)

        riscProgram.append(instr);
    }

    /*   Register lhs;
     *   Register rhs; */
    @Override
    public void visit(Move_Reg_Reg n) {
        /* Deconstruct Sparrow-V instruction components */
        String lhs = n.lhs.toString();
        String rhs = n.rhs.toString();

        /* Build RISC-V instruction */
        String instr = "  mv " + lhs + ", " + rhs + "\n";                                  // mv t0, t1

        riscProgram.append(instr);
    }

    /*   Identifier lhs;
     *   Register rhs; */
    @Override
    public void visit(Move_Id_Reg n) {
        /* Deconstruct Sparrow-V instruction components */
        String lhs = n.lhs.toString();
        String rhs = n.rhs.toString();

        /* Get offset for this identifier */
        int offset = currFuncMetadata.hasVar(lhs) ? currFuncMetadata.getVarOffsets().get(lhs) : currFuncMetadata.getArgOffsets().get(lhs);

        /* Build RISC-V instruction */
        String instr = "  sw " + rhs + ", " + offset + "(fp)\n";
        riscProgram.append(instr);
    }

    /*   Register lhs;
     *   Identifier rhs; */
    @Override
    public void visit(Move_Reg_Id n) {
        /* Deconstruct Sparrow-V instruction comoennts */
        String lhs = n.lhs.toString();
        String rhs = n.rhs.toString();

        /* Get offset for this identifier */
        int offset = currFuncMetadata.hasVar(rhs) ? currFuncMetadata.getVarOffsets().get(rhs) : currFuncMetadata.getArgOffsets().get(rhs);

        /* Build RISC-V instruction */
        String instr = "  lw " + lhs + ", " + offset + "(fp)\n";
        riscProgram.append(instr);
    }

    /*   Register lhs;
     *   Register size; */
    @Override
    public void visit(Alloc n) {
        /* Deconstruct Sparrow-V instruction components */
        String lhs = n.lhs.toString();
        String size = n.size.toString();

        /* Build RISC-V instructions */
        String firstInstr = "  mv a0, " + size + "\n";
        String lastInstr = "  mv " + lhs + ", a0" + "\n";

        riscProgram.append(firstInstr);                                                     // mv a0, t1
        riscProgram.append("  jal alloc\n");                                            // jal alloc
        riscProgram.append(lastInstr);                                                      // mv t0, a0
    }

    /*   Register content; */
    @Override
    public void visit(Print n) {
        /* Deconstruct Sparrow-V instruction component */
        String content = n.content.toString();

        /* Build RISC-V instructions */
        String instr = "  mv a0, " + content + "\n";
        riscProgram.append(instr);                                                          // mv a0, t0
        riscProgram.append("  jal print\n");                                            // jal print
    }

    /*   String msg; */
    @Override
    public void visit(ErrorMessage n) {
        /* Build RISC-V instructions */
        riscProgram.append("  la a0, msg_nullptr\n");                                  // la a0, msg_nullptr
        riscProgram.append("  jal error\n");                                            // jal error
    }

    /*   Label label; */
    @Override
    public void visit(Goto n) {
        /* Deconstruct Sparrow-V instruction component */
        String label = n.label.toString();

        String mangled = currentFunction + "_" + label;

        /* Build RISC-V instruction */
        String instr = "  jal " + mangled + "\n";
        riscProgram.append(instr);
    }

    /*   Register condition;
     *   Label label; */
    @Override
    public void visit(IfGoto n) {
        /* Deconstruct Sparrow-V instruction component */
        String cond = n.condition.toString();
        String label = n.label.toString();

        String mangled = currentFunction + "_" + label;

        String skip = mangled + "_no_jump_" + count++;

        /* Build RISC-V instructions */
        String firstInstr = "  bnez " + cond + ", " + skip + "\n";
        String secondInstr = "  jal " + mangled + "\n";
        String thirdInstr = skip + ":\n";

        riscProgram.append(firstInstr);
        riscProgram.append(secondInstr);
        riscProgram.append(thirdInstr);
    }

    /*   Register lhs;
     *   Register callee;
     *   List<Identifier> args; */
    @Override
    public void visit(Call n) {
        int numArgs = n.args.size();

        /* Build RISC-V instructions */
        String firstInstr = "  li t6, " + (numArgs * 4) + "\n";
        String moveSp = "  sub sp, sp, t6\n";

        riscProgram.append(firstInstr);
        riscProgram.append(moveSp);
        
        /* Load call arguments */
        int spOffset = 0;
        for (Identifier a : n.args) {
            String argName = a.toString();
            int offset = currFuncMetadata.hasVar(argName) ? currFuncMetadata.getVarOffsets().get(argName) : currFuncMetadata.getArgOffsets().get(argName);

            String instrLoad = "  lw t6, " + offset + "(fp)\n";
            String instrStore = "  sw t6, " + spOffset + "(sp)\n";

            riscProgram.append(instrLoad);
            riscProgram.append(instrStore);

            spOffset += 4;
        }

        /* Make call */
        String doCall = "  jalr " + n.callee.toString() + "\n";
        riscProgram.append(doCall);

        /* Reset stack pointer */
        String resetSp = "  addi sp, sp, " + (numArgs * 4) + "\n";
        String storeReturn = "  mv " + n.lhs.toString() + ", a0\n";

        riscProgram.append(resetSp);
        riscProgram.append(storeReturn);
    }

}
