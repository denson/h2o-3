package water.api;

import hex.Interpret;
import water.Key;
import water.api.schemas3.KeyV3;
import water.api.schemas3.SchemaV3;

/**
 * Interpret Job Schema
 */
public class InterpretV3 extends SchemaV3<Interpret, InterpretV3> {
  @SuppressWarnings("unused")
  @API(help="Model", direction = API.Direction.INOUT)
  public KeyV3.ModelKeyV3 model_id;

  @SuppressWarnings("unused")
  @API(help="Frame", direction=API.Direction.INOUT)
  public KeyV3.FrameKeyV3 frame_id;

  @SuppressWarnings("unused")
  @API(help="Frame", direction=API.Direction.INOUT)
  public KeyV3.FrameKeyV3 klime_frame_id;

  @SuppressWarnings("unused")
  @API(help="Interpret Frame", direction=API.Direction.OUTPUT)
  public KeyV3.FrameKeyV3 interpret_id;

  @SuppressWarnings("unused")
  @API(help="Intercept Coefficients for clusters", direction=API.Direction.OUTPUT)
  public double[] cluster_intercepts;

  @SuppressWarnings("unused")
  @API(help="R2s for clusters", direction=API.Direction.OUTPUT)
  public double[] cluster_r2s;

  @SuppressWarnings("unused")
  @API(help="R2 for global klime", direction=API.Direction.OUTPUT)
  public double global_r2;

  @SuppressWarnings("unused")
  @API(help="Intercept for global klime", direction=API.Direction.OUTPUT)
  public double global_intercept;

  @API(help="Key to store the destination", direction=API.Direction.INPUT)
  public InterpretKeyV3 destination_key;

  @Override public Interpret createImpl( ) { return new Interpret(Key.<Interpret>make()); }
}