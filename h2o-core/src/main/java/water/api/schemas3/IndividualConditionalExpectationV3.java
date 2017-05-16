package water.api.schemas3;

import hex.IndividualConditionalExpectation;
import water.Key;
import water.api.API;

/**
 *
 */
public class IndividualConditionalExpectationV3 extends SchemaV3<IndividualConditionalExpectation, IndividualConditionalExpectationV3> {
    @SuppressWarnings("unused")
    @API(help="Model", direction = API.Direction.INOUT)
    public KeyV3.ModelKeyV3 model_id;

    @SuppressWarnings("unused")
    @API(help="Frame", direction=API.Direction.INOUT)
    public KeyV3.FrameKeyV3 frame_id;

    @SuppressWarnings("unused")
    @API(help="Frame", direction=API.Direction.INOUT)
    public long row;

    @SuppressWarnings("unused")
    @API(help="Column(s)", direction=API.Direction.INOUT)
    public String[] cols;

    @SuppressWarnings("unused")
    @API(help="Number of bins", direction=API.Direction.INOUT)
    public int nbins;

    @SuppressWarnings("unused")
    @API(help="Individual Conditional Expectation Data", direction=API.Direction.OUTPUT)
    public TwoDimTableV3[] ind_cond_exp_data;

    @API(help="Key to store the destination", direction=API.Direction.INPUT)
    public KeyV3.IndividualConditionalExpectationKeyV3 destination_key;

    @Override public IndividualConditionalExpectation createImpl( ) { return new IndividualConditionalExpectation(Key.<IndividualConditionalExpectation>make()); }
}