#!/bin/bash

# Check if input file is provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 <input.java>"
    exit 1
fi

INPUT_FILE="$1"
BASENAME=$(basename "$INPUT_FILE" .java)
OUTPUT_DIR="pipeline_output"

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo "=== MiniJava to RISC-V Pipeline ==="
echo "Input: $INPUT_FILE"
echo "Output directory: $OUTPUT_DIR"
echo

# Stage 1 + 2: Parse + Typecheck (hw2)
echo "Stage 2: Type checking..."
TYPECHECK_OUTPUT="$OUTPUT_DIR/${BASENAME}.txt"
gradle run -Phomework=hw2 -q < "$INPUT_FILE" > "$TYPECHECK_OUTPUT" 2>&1
if [ $? -eq 0 ]; then
    echo "✓ Type checking successful"
else
    echo "✗ Type checking failed"
    cat "$TYPECHECK_OUTPUT"
    exit 1
fi

# Stage 3: J2S (hw3) - MiniJava to Sparrow
echo "Stage 3: Translating MiniJava to Sparrow..."
SPARROW_OUTPUT="$OUTPUT_DIR/${BASENAME}.sparrow"
gradle run -Phomework=hw3 -q < "$INPUT_FILE" > "$SPARROW_OUTPUT" 2>&1
if [ $? -eq 0 ]; then
    echo "✓ Translation to Sparrow successful"
    echo "  Output size: $(wc -l < "$SPARROW_OUTPUT") lines"
else
    echo "✗ Translation to Sparrow failed"
    cat "$SPARROW_OUTPUT"
    exit 1
fi

# Stage 4: S2SV (hw4) - Sparrow to Sparrow-V
echo "Stage 4: Translating Sparrow to Sparrow-V..."
SPARROWV_OUTPUT="$OUTPUT_DIR/${BASENAME}.sparrowv"
gradle run -Phomework=hw4 -q < "$SPARROW_OUTPUT" > "$SPARROWV_OUTPUT" 2>&1
if [ $? -eq 0 ]; then
    echo "✓ Translation to Sparrow-V successful"
    echo "  Output size: $(wc -l < "$SPARROWV_OUTPUT") lines"
else
    echo "✗ Translation to Sparrow-V failed"
    cat "$SPARROWV_OUTPUT"
    exit 1
fi

# Stage 5: SV2V (hw5) - Sparrow-V to RISC-V
echo "Stage 5: Translating Sparrow-V to RISC-V..."
RISCV_OUTPUT="$OUTPUT_DIR/${BASENAME}.riscv"
gradle run -Phomework=hw5 -q < "$SPARROWV_OUTPUT" > "$RISCV_OUTPUT" 2>&1
if [ $? -eq 0 ]; then
    echo "✓ Translation to RISC-V successful"
    echo "  Output size: $(wc -l < "$RISCV_OUTPUT") lines"
else
    echo "✗ Translation to RISC-V failed"
    cat "$RISCV_OUTPUT"
    exit 1
fi

echo
echo "=== Pipeline Complete ==="
echo "Final RISC-V output: $RISCV_OUTPUT"
echo
echo "Generated files:"
echo "  - Parse + Typecheck output: $TYPECHECK_OUTPUT"
echo "  - Sparrow IR: $SPARROW_OUTPUT"
echo "  - Sparrow-V IR: $SPARROWV_OUTPUT"
echo "  - RISC-V assembly: $RISCV_OUTPUT"
echo

# Run the RISC-V code if Venus is available
if [ -f "misc/venus.jar" ]; then
    echo "=== Running RISC-V Code ==="
    EXECUTION_OUTPUT="$OUTPUT_DIR/${BASENAME}.out"
    java -jar misc/venus.jar < "$RISCV_OUTPUT" > "$EXECUTION_OUTPUT" 2>&1
    echo "Execution output saved to: $EXECUTION_OUTPUT"
    echo "Output:"
    cat "$EXECUTION_OUTPUT"
fi