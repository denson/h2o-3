package hex;

import jsr166y.CountedCompleter;
import water.*;
import water.api.schemas3.KeyV3;
import water.fvec.Frame;
import water.fvec.Vec;
import water.util.Log;
import water.util.TwoDimTable;
import water.rapids.Rapids;
import java.util.Arrays;


public class IndividualConditionalExpectation extends Lockable<IndividualConditionalExpectation> {
    transient final public Job _job;
    public Key<Model> _model_id;
    public Key<Frame> _frame_id;
    public long _row;
    public String[] _cols;
    public int _nbins = 20;
    public TwoDimTable[] _ind_cond_exp_data; //OUTPUT

    public IndividualConditionalExpectation(Key<IndividualConditionalExpectation> dest) {
        super(dest);
        _job = new Job<>(dest, IndividualConditionalExpectation.class.getName(), "IndividualConditionalExpectation");
    }

    public Job<IndividualConditionalExpectation> execImpl() {
        checkSanityAndFillParams();
        delete_and_lock(_job);
        _frame_id.get().write_lock(_job._key);
        // Don't lock the model since the act of unlocking at the end would
        // freshen the DKV version, but the live POJO must survive all the way
        // to be able to delete the model metrics that got added to it.
        // Note: All threads doing the scoring call model_id.get() and then
        // update the _model_metrics only on the temporary live object, not in DKV.
        // At the end, we call model.remove() and we need those model metrics to be
        // deleted with it, so we must make sure we keep the live POJO alive.
        _job.start(new IndividualConditionalExpectationDriver(), _cols.length);
        return _job;
    }

    private void checkSanityAndFillParams() {
        if (_cols==null) {
            Model m = _model_id.get();
            if (m==null) throw new IllegalArgumentException("Model not found.");
            if (!m._output.isSupervised() || m._output.nclasses() > 2)
                throw new IllegalArgumentException("Individual Conditional Expectation plots are only implemented for regression and binomial classification models");
            Frame f = _frame_id.get();
            if (f==null) throw new IllegalArgumentException("Frame not found.");

            if (Model.GetMostImportantFeatures.class.isAssignableFrom(m.getClass())) {
                _cols = ((Model.GetMostImportantFeatures)m).getMostImportantFeatures(10);
                if (_cols != null) {
                    Log.info("Selecting the top " + _cols.length + " features from the model's variable importances");
                }
            }
        }
        if (_nbins < 2) {
            throw new IllegalArgumentException("_nbins must be >=2.");
        }
        final Frame fr = _frame_id.get();
        for (int i = 0; i < _cols.length; ++i) {
            final String col = _cols[i];
            Vec v = fr.vec(col);
            if (v.isCategorical() && v.cardinality() > _nbins) {
                throw new IllegalArgumentException("Column " + col + "'s cardinality of " + v.cardinality() + " > nbins of " + _nbins);
            }
        }
    }

    private class IndividualConditionalExpectationDriver extends H2O.H2OCountedCompleter<IndividualConditionalExpectationDriver> {
        public void compute2() {
            assert (_job != null);
            final Frame fr = _frame_id.get();
            // loop over PDPs (columns)
            _ind_cond_exp_data = new TwoDimTable[_cols.length];
            for (int i = 0; i < _cols.length; ++i) {
                final String col = _cols[i];
                Log.debug("Computing individual conditional expectation of model on '" + col + "'.");
                Vec v = fr.vec(col);
                int actualbins = _nbins;
                if (v.isInt() && (v.max() - v.min() + 1) < _nbins) {
                    actualbins = (int) (v.max() - v.min() + 1);
                }
                double[] colVals = new double[actualbins];
                double delta = (v.max() - v.min()) / (actualbins - 1);
                if (actualbins == 1) delta = 0;
                for (int j = 0; j < colVals.length; ++j) {
                    colVals[j] = v.min() + j * delta;
                }
                Log.debug("Computing IndividualConditionalExpectation for column " + col + " at the following values: ");
                Log.debug(Arrays.toString(colVals));

                Futures fs = new Futures();
                final double response[] = new double[colVals.length];

                final boolean cat = fr.vec(col).isCategorical();

                // loop over column values (fill one IndividualConditionalExpectation)
                for (int k = 0; k < colVals.length; ++k) {
                    final double value = colVals[k];
                    final int which = k;
                    H2O.H2OCountedCompleter ice = new H2O.H2OCountedCompleter() {
                        @Override
                        public void compute2() {
                            Frame frRow = Rapids.exec("(rows " + _frame_id + "  " + _row + ")").getFrame();
                            Frame test = new Frame(frRow.names(), frRow.vecs());
                            Vec orig = test.remove(col);
                            Vec cons = orig.makeCon(value);
                            if (cat) cons.setDomain(frRow.vec(col).domain());
                            test.add(col, cons);
                            Frame preds = null;
                            try {
                                preds = _model_id.get().score(test, Key.make().toString(), _job, false);
                                if (_model_id.get()._output.nclasses() == 2) {
                                    response[which] = preds.vec(2).at(0); //Only dealing with a row...
                                } else if (_model_id.get()._output.nclasses() == 1) {
                                    response[which] = preds.vec(0).at(0); //Only dealing with a row...
                                } else throw H2O.unimpl();
                            } finally {
                                if (preds != null) preds.remove();
                            }
                            cons.remove();
                            tryComplete();
                        }
                    };
                    fs.add(H2O.submitTask(ice));
                }
                fs.blockForPending();

                _ind_cond_exp_data[i] = new TwoDimTable("IndividualConditionalExpectation",
                        ("Individual Conditional Expectation Plot of model " + _model_id + " on column '" + _cols[i] + "'" + " on row " + _row),
                        new String[actualbins],
                        new String[]{_cols[i], "response"},
                        new String[]{cat ? "string" : "double", "double"},
                        new String[]{cat ? "%s" : "%5f", "%5f"}, null);
                for (int j = 0; j < response.length; ++j) {
                    if (fr.vec(col).isCategorical()) {
                        _ind_cond_exp_data[i].set(j, 0, fr.vec(col).domain()[(int) colVals[j]]);
                    } else {
                        _ind_cond_exp_data[i].set(j, 0, colVals[j]);
                    }
                    _ind_cond_exp_data[i].set(j, 1, response[j]);
                }
                _job.update(1);
                update(_job);
                if (_job.stop_requested())
                    break;
            }
            tryComplete();
        }

        @Override
        public void onCompletion(CountedCompleter caller) {
            _frame_id.get().unlock(_job._key);
            unlock(_job);
        }

        @Override
        public boolean onExceptionalCompletion(Throwable ex, CountedCompleter caller) {
            _frame_id.get().unlock(_job._key);
            unlock(_job);
            return true;
        }
    }
    @Override public Class<KeyV3.IndividualConditionalExpectationKeyV3> makeSchema() { return KeyV3.IndividualConditionalExpectationKeyV3.class; }
}

