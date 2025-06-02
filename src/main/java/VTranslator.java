
import IR.token.Identifier;
import sparrowv.Add;
import sparrowv.Alloc;
import sparrowv.Block;
import sparrowv.Call;
import sparrowv.ErrorMessage;
import sparrowv.FunctionDecl;
import sparrowv.Goto;
import sparrowv.IfGoto;
import sparrowv.Instruction;
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

    StringBuilder riscProgram;  // RISC-V program

    public VTranslator() {
        riscProgram = new StringBuilder();
    }

    // Sparrow-V AST components

    /*   List<FunctionDecl> funDecls; */
    @Override
    public void visit(Program n) {
        /* Add program header */ 
        riscProgram.append(".equiv @sbrk, 9\n"); // .equiv @sbrk, 9
        riscProgram.append(".equiv @print_string, 4\n"); // .equiv @print_string, 4
        riscProgram.append(".equiv @print_char, 11\n"); // .equiv @print_char, 11
        riscProgram.append(".equiv @print_int, 1\n"); // .equiv @print_int, 1
        riscProgram.append(".equiv @exit, 10\n"); // .equiv @exit, 10
        riscProgram.append(".equiv @exit2, 17\n"); // .equiv @exit2, 17
        riscProgram.append("\n");

        riscProgram.append(".text\n"); // .text
        riscProgram.append("\n");

        /* RISC-V entry point */
        riscProgram.append(".globl main\n"); // .globl main
        riscProgram.append("\tjal Main\n"); // jal Main
        riscProgram.append("\t li a0, @exit\n"); // li a0, @exit
        riscProgram.append("\tecall\n"); // ecall
        riscProgram.append("\n");

        /* Sparrow-V Main function */
        riscProgram.append(".globl Main\n"); // .globl Main
        riscProgram.append("Main:\n"); // Main:
        
        for (FunctionDecl fd : n.funDecls) {
            fd.accept(this);
        }

        /* Print */
        riscProgram.append(".globl print\n"); // .globl print
        riscProgram.append("print:\n"); // print:
        riscProgram.append("\tmv a1, a0\n"); // mv a1, a0
        riscProgram.append("\tli a0, @print_int\n"); // li a0, @print_int
        riscProgram.append("\tecall\n"); // ecall
        riscProgram.append("\tli a1, 10\n"); // li a1, 10
        riscProgram.append("\tli a0, @print_char\n"); // li a0, @print_char
        riscProgram.append("\tecall\n"); // ecall
        riscProgram.append("\tjr ra\n"); // jr ra
        riscProgram.append("\n");

        /* Error */
        riscProgram.append(".globl error\n"); // .globl error
        riscProgram.append("error:\n"); // error:
        riscProgram.append("\tmv a1, a0\n"); // mv a1, a0
        riscProgram.append("\tli a0, @print_string\n"); // li a0, @print_string
        riscProgram.append("\tecall\n"); // ecall
        riscProgram.append("\tli a1, 10\n"); // li a1, 10
        riscProgram.append("\tli a0, @print_char\n"); // li a0, @print_char
        riscProgram.append("\tecall\n"); // ecall
        riscProgram.append("\tli a0, @exit\n"); // li a0, @exit
        riscProgram.append("\tecall\n"); // ecall
        riscProgram.append("abort_17:\n"); // abort_17:
        riscProgram.append("\t j abort_17\n"); // j abort_17
        riscProgram.append("\n");

        /* Alloc */
        riscProgram.append(".globl alloc\n"); // .globl alloc
        riscProgram.append("alloc:\n"); // alloc:
        riscProgram.append("\tmv a1, a0\n"); // mv a1, a0
        riscProgram.append("\tli a0, @sbrk\n"); // li a0, @sbrk
        riscProgram.append("\tecall\n"); // ecall
        riscProgram.append("\tjr ra\n"); // jr ra
        riscProgram.append("\n");
        
        riscProgram.append(".data\n"); // .data
        riscProgram.append("\n");

        /* Null pointer */
        riscProgram.append(".globl msg_nullptr\n"); // .globl msg_nullptr
        riscProgram.append("msg_nullptr:\n"); // msg_nullptr
        riscProgram.append("\t.asciiz \"null pointer\"\n"); // .asciiz "null pointer"
        riscProgram.append("\t.align 2\n"); // .align 2
        riscProgram.append("\n");

        /* Array index out of bounds */
        riscProgram.append(".globl msg_array_oob\n"); // .globl msg_array_oob
        riscProgram.append("msg_array_oob:\n"); // msg_array_oob
        riscProgram.append(".asciiz \"array index out of bounds\"\n"); // .asciiz "array index out of bounds"
        riscProgram.append(".align 2\n"); // .align 2
    }

    /*   Program parent;
     *   FunctionName functionName;
     *   List<Identifier> formalParameters;
     *   Block block; */
    @Override
    public void visit(FunctionDecl n) {
        for (Identifier fp : n.formalParameters) {
            // ... fp ...
        }
        n.block.accept(this);
    }

    /*   FunctionDecl parent;
     *   List<Instruction> instructions;
     *   Identifier return_id; */
    public void visit(Block n) {
        for (Instruction i : n.instructions) {
            i.accept(this);
        }
    }

    /*   Label label; */
    public void visit(LabelInstr n) {
    }

    /*   Register lhs;
     *   int rhs; */
    @Override
    public void visit(Move_Reg_Integer n) {
        /* Deconstruct Sparrow-V instruction components */
        String lhs = n.lhs.toString();
        int rhs = n.rhs;

        /* Build RISC-V instruction */
        String instr = "\tli " + lhs + ", " + rhs + "\n";
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
        String instr = "\tla " + lhs + ", @" + rhs + "\n";
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
        String instr = "\tadd " + lhs + ", " + arg1 + ", " + arg2 + "\n";

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
        String instr = "\tsub " + lhs + ", " + arg1 + ", " + arg2 + "\n";

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
        String instr = "\tmul " + lhs + ", " + arg1 + ", " + arg2 + "\n";

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
        String instr = "\tslt " + lhs + ", " + arg1 + ", " + arg2 + "\n";

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
        String instr = "\tlw " + lhs + ", " + offset + "(" + base + ")" + "\n";

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
        String instr = "\tsw " + rhs + ", " + offset + "(" + base + ")" + "\n";

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
        String instr = "\tmov " + lhs + ", " + rhs + "\n";

        riscProgram.append(instr);
    }

    /*   Identifier lhs;
     *   Register rhs; */
    public void visit(Move_Id_Reg n) {
        // TODO
    }

    /*   Register lhs;
     *   Identifier rhs; */
    public void visit(Move_Reg_Id n) {
        // TODO
    }

    /*   Register lhs;
     *   Register size; */
    @Override
    public void visit(Alloc n) {
        /* Deconstruct Sparrow-V instruction components */
        String lhs = n.lhs.toString();
        String size = n.size.toString();

        /* Build RISC-V instructions */
        String firstInstr = "\tmv a0, " + size + "\n";
        String lastInstr = "\tmv " + lhs + ", a0" + "\n";

        riscProgram.append(firstInstr);
        riscProgram.append("\tjal alloc\n");
        riscProgram.append(lastInstr);
    }

    /*   Register content; */
    @Override
    public void visit(Print n) {
        /* Deconstruct Sparrow-V instruction component */
        String content = n.content.toString();

        /* Build RISC-V instructions */
        String instr = "mv a0, " + content + "\n";
        riscProgram.append(instr);
        riscProgram.append("\tjal print\n");
    }

    /*   String msg; */
    @Override
    public void visit(ErrorMessage n) {
        /* Build RISC-V instructions */
        riscProgram.append("\tla a0, null_pointer\n");
        riscProgram.append("\tjal error\n");
    }

    /*   Label label; */
    public void visit(Goto n) {
        // TODO
    }

    /*   Register condition;
     *   Label label; */
    public void visit(IfGoto n) {
    }

    /*   Register lhs;
     *   Register callee;
     *   List<Identifier> args; */
    public void visit(Call n) {
    }

}
