package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.general.BoxedObject;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.gui.tabpanels.SpectrumIdentificationPanel;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.preferences.DisplayParameters;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import java.sql.SQLNonTransientConnectionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Table model for a set of peptide to spectrum matches.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PsmTableModel extends SelfUpdatingTableModel {

    /**
     * The identification of this project.
     */
    private Identification identification;
    /**
     * The display features generator.
     */
    private DisplayFeaturesGenerator displayFeaturesGenerator;
    /**
     * The ID input map.
     */
    private InputMap inputMap;
    /**
     * The identification parameters.
     */
    private IdentificationParameters identificationParameters;
    /**
     * A list of ordered PSM keys.
     */
    private ArrayList<String> psmKeys = null;
    /**
     * Indicates whether the scores should be displayed instead of the
     * confidence
     */
    private boolean showScores = false;
    /**
     * The batch size.
     */
    private int batchSize = 20;
    /**
     * The exception handler catches exceptions.
     */
    private ExceptionHandler exceptionHandler;

    /**
     * Constructor which sets a new table.
     *
     * @param identification the identification object containing the matches
     * @param displayFeaturesGenerator the display features generator
     * @param identificationParameters the identification parameters
     * @param psmKeys the PSM keys
     * @param displayScores boolean indicating whether the scores should be
     * displayed instead of the confidence
     * @param exceptionHandler handler for the exceptions
     */
    public PsmTableModel(Identification identification, DisplayFeaturesGenerator displayFeaturesGenerator, IdentificationParameters identificationParameters,
            ArrayList<String> psmKeys, boolean displayScores, ExceptionHandler exceptionHandler) {
        this.identification = identification;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.identificationParameters = identificationParameters;
        this.psmKeys = psmKeys;
        this.showScores = displayScores;
        this.exceptionHandler = exceptionHandler;

        PSMaps pSMaps = new PSMaps();
        pSMaps = (PSMaps) identification.getUrParam(pSMaps);
        inputMap = pSMaps.getInputMap();
    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table.
     *
     * @param identification the identification object containing the matches
     * @param displayFeaturesGenerator the display features generator
     * @param identificationParameters the identification parameters
     * @param psmKeys the PSM keys
     * @param displayScores boolean indicating whether the scores should be
     * displayed instead of the confidence
     */
    public void updateDataModel(Identification identification, DisplayFeaturesGenerator displayFeaturesGenerator, IdentificationParameters identificationParameters,
            ArrayList<String> psmKeys, boolean displayScores) {
        this.identification = identification;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.identificationParameters = identificationParameters;
        this.psmKeys = psmKeys;
        this.showScores = displayScores;

        PSMaps pSMaps = new PSMaps();
        pSMaps = (PSMaps) identification.getUrParam(pSMaps);
        inputMap = pSMaps.getInputMap();
    }

    /**
     * Resets the peptide keys.
     */
    public void reset() {
        psmKeys = null;
    }

    /**
     * Constructor which sets a new empty table.
     */
    public PsmTableModel() {
    }

    @Override
    public int getRowCount() {
        if (psmKeys != null) {
            return psmKeys.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getColumnCount() {
        return 8;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return " ";
            case 1:
                return "  ";
            case 2:
                return "ID";
            case 3:
                return "Sequence";
            case 4:
                return "Charge";
            case 5:
                return "m/z Error";
            case 6:
                if (showScores) {
                    return "Score";
                } else {
                    return "Confidence";
                }
            case 7:
                return "";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int row, int column) {

        try {
            int viewIndex = getViewIndex(row);

            if (viewIndex < psmKeys.size()) { // escape possible null pointer

                String psmKey = psmKeys.get(viewIndex);
                boolean useDB = !isSelfUpdating();

                SpectrumMatch spectrumMatch = (SpectrumMatch) identification.retrieveObject(psmKey);
                switch (column) {
                    case 0:
                        return viewIndex + 1;
                    case 1:
                        PSParameter psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
                        if (psParameter == null) {
                            if (isScrolling()) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayParameters.LOADING_MESSAGE;
                            }
                        }
                        return psParameter.getStarred();
                    case 2:
                        if (spectrumMatch == null) {
                            if (isScrolling()) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayParameters.LOADING_MESSAGE;
                            }
                        }

                        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = spectrumMatch.getAssumptionsMap();
                        return SpectrumIdentificationPanel.isBestPsmEqualForAllIdSoftware(spectrumMatch, assumptions, identificationParameters.getSequenceMatchingParameters(), inputMap.getInputAlgorithmsSorted().size());
                    case 3:
                        if (spectrumMatch == null) {
                            if (isScrolling()) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayParameters.LOADING_MESSAGE;
                            }
                        }
                        return displayFeaturesGenerator.getTaggedPeptideSequence(spectrumMatch, true, true, true);
                    case 4:
                        if (spectrumMatch == null) {
                            if (isScrolling()) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayParameters.LOADING_MESSAGE;
                            }
                        }
                        if (spectrumMatch.getBestPeptideAssumption() != null) {
                            return spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
                        } else if (spectrumMatch.getBestTagAssumption() != null) {
                            return spectrumMatch.getBestTagAssumption().getIdentificationCharge().value;
                        } else {
                            throw new IllegalArgumentException("No best assumption found for spectrum " + psmKey + ".");
                        }
                    case 5:
                        if (spectrumMatch == null) {
                            if (isScrolling()) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayParameters.LOADING_MESSAGE;
                            }
                        }
                        Precursor precursor = SpectrumFactory.getInstance().getPrecursor(psmKey);
                        SearchParameters searchParameters = identificationParameters.getSearchParameters();
                        if (spectrumMatch.getBestPeptideAssumption() != null) {
                            return Math.abs(spectrumMatch.getBestPeptideAssumption().getDeltaMass(precursor.getMz(), searchParameters.isPrecursorAccuracyTypePpm(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()));
                        } else if (spectrumMatch.getBestTagAssumption() != null) {
                            return Math.abs(spectrumMatch.getBestTagAssumption().getDeltaMass(precursor.getMz(), searchParameters.isPrecursorAccuracyTypePpm(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()));
                        } else {
                            throw new IllegalArgumentException("No best assumption found for spectrum " + psmKey + ".");
                        }
                    case 6:
                        psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
                        if (psParameter == null) {
                            if (isScrolling) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayParameters.LOADING_MESSAGE;
                            }
                        }
                        if (psParameter != null) {
                            if (showScores) {
                                return psParameter.getPsmScore();
                            } else {
                                return psParameter.getPsmConfidence();
                            }
                        } else {
                            return null;
                        }
                    case 7:
                        psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
                        if (psParameter == null) {
                            if (isScrolling) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayParameters.LOADING_MESSAGE;
                            }
                        }
                        if (psParameter != null) {
                            return psParameter.getMatchValidationLevel().getIndex();
                        } else {
                            return null;
                        }
                    default:
                        return null;
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            if (exceptionHandler != null) {
                exceptionHandler.catchException(e);
            } else {
                throw new IllegalArgumentException("Table not instantiated.");
            }
            return "";
        }
    }

    /**
     * Indicates whether the table content was instantiated.
     *
     * @return a boolean indicating whether the table content was instantiated.
     */
    public boolean isInstantiated() {
        return identification != null;
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        for (int i = 0; i < getRowCount(); i++) {
            if (getValueAt(i, columnIndex) != null) {
                return getValueAt(i, columnIndex).getClass();
            }
        }
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    protected void catchException(Exception e) {
        setSelfUpdating(false);
        exceptionHandler.catchException(e);
    }

    @Override
    protected int loadDataForRows(ArrayList<Integer> rows, WaitingHandler waitingHandler) {

        ArrayList<String> keys = rows.stream().map(i -> psmKeys.get(i)).collect(Collectors.toCollection(ArrayList::new));
        
        try {

            identification.loadObjects(keys, waitingHandler, false);
            return rows.get(rows.size() - 1);

        } catch (SQLNonTransientConnectionException e) {
            // connection has been closed
            return rows.get(0);
        } catch (Exception e) {
            catchException(e);
            return rows.get(0);
        }
    }

    @Override
    protected void loadDataForColumn(int column, WaitingHandler waitingHandler) {
        try {
            if (column == 1
                    || column == 6
                    || column == 7) {
                identification.loadObjects(psmKeys, waitingHandler, false);
            } else if (column == 2
                    || column == 3
                    || column == 4
                    || column == 5) {
                identification.loadObjects(psmKeys, waitingHandler, false);
            }
        } catch (Exception e) {
            catchException(e);
        }
    }
}
