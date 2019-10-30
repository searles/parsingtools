import at.searles.parsing.ParserStream
import org.junit.Test

class MeelanSrcTest {
    /*
    // what is returned:
value.x/value.y are normalized to the range 0-1.
layer is then used in value.x where layer is also normalized.
Thus, in total 3 values are returned.

float3(value.x [with layer], value.y, height) These values are then also stored.
     */

    @Test
    fun testSimpleMandel() {
        val ast = test.Meelan.program.parse(ParserStream.fromString(simpleProgram));
        val source = test.Meelan.program.print(ast)
    }

    val simpleProgram = """
// very simple mandelbrot set
scale = [2,0,0,2,0,0]
layers = {
    lake = [[#ff000000, #ffffffff], [#ffffffff, #ff000000]],
    exterior = [[#ffff0000, #ffffff00, #ffffffff, #ff0000ff]]
};

// I need extern booleans

extern isJulia {en="Julia Set"} = "false";
extern juliaParameter {en="Julia Set Parameter"} = "-1";

var c = if(!isJulia) point() else juliaParameter; // function to return the scaled point.
// other functions are dim() and pixel() which return the current size.

extern maxIter {en="Maximum Number of Iterations"} = "100"; // will be shown as "Maximum Number of Iterations (maxIterations)"

extern init {en="Start Value"} = "0";

var i = 0;
var z: Cplx = if(!isJulia) init else point();

while {
    extern function {en = "Function"} = "sqr z + c";
    z = function;
    
    if(rad2 z > bailout) {
        result(0, i: arc z, rad z);
        false
    } else if(i >= maxIter) {
        result(1, z, rad z);
        false
    } else {
        i = i + 1;
        true
    }
}
""".trimIndent()
}