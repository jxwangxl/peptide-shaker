package eu.isas.peptideshaker;

import com.compomics.util.experiment.biology.enzymes.EnzymeFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.db.object.ObjectsDB;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.ProjectParameters;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.amino_acid_tags.Tag;
import com.compomics.util.experiment.identification.protein_inference.FastaMapper;
import com.compomics.util.experiment.io.biology.protein.FastaParameters;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import eu.isas.peptideshaker.fileimport.FileImporter;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.parameters.identification.advanced.FractionParameters;
import com.compomics.util.parameters.identification.advanced.IdMatchValidationParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import eu.isas.peptideshaker.scoring.PSMaps;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.parameters.identification.advanced.ModificationLocalizationParameters;
import com.compomics.util.parameters.tools.ProcessingParameters;
import com.compomics.util.parameters.identification.advanced.PsmScoringParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.UtilitiesUserParameters;
import com.compomics.util.waiting.Duration;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import com.compomics.util.parameters.quantification.spectrum_counting.SpectrumCountingParameters;
import eu.isas.peptideshaker.protein_inference.ProteinInference;
import eu.isas.peptideshaker.ptm.ModificationLocalizationScorer;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.psm_scoring.BestMatchSelection;
import eu.isas.peptideshaker.scoring.psm_scoring.PsmScorer;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import com.compomics.util.experiment.identification.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.identification.peptide_shaker.Metrics;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.parameters.peptide_shaker.ProjectType;
import eu.isas.peptideshaker.protein_inference.PeptideChecker;
import eu.isas.peptideshaker.validation.MatchesValidator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;

/**
 * This class will be responsible for the identification import and the
 * associated calculations.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideShaker {

    /**
     * The experiment conducted.
     */
    private ProjectParameters projectParameters;
    /**
     * The validator which will take care of the matches validation
     */
    private MatchesValidator matchesValidator;
    /**
     * The PTM scorer responsible for scoring PTM localization.
     */
    private ModificationLocalizationScorer ptmScorer;
    /**
     * The id importer will import and process the identifications.
     */
    private FileImporter fileImporter = null;
    /**
     * User preferences file.
     */
    private static String USER_PREFERENCES_FILE = System.getProperty("user.home") + "/.peptideshaker/userpreferences_2.0.cpf";
    /**
     * Default PeptideShaker modifications.
     */
    public static final String PEPTIDESHAKER_CONFIGURATION_FILE = "PeptideShaker_configuration.txt";
    /**
     * The location of the folder used for the database.
     */
    private static String DATABASE_DIRECTORY = "matches";
    /**
     * Folder where the data files are stored by default. Should be the same as
     * in SearchGUI.
     */
    public static String DATA_DIRECTORY = "data";
    /**
     * The parent directory of the serialization directory. An empty string if
     * not set.
     */
    private static String SERIALIZATION_PARENT_DIRECTORY = "resources";
    /**
     * The compomics PTM factory.
     */
    private ModificationFactory modificationFactory = ModificationFactory.getInstance();
    /**
     * Metrics to be picked when loading the identification.
     */
    private Metrics metrics = new Metrics();
    /**
     * The gene maps.
     */
    private GeneMaps geneMaps = new GeneMaps();
    /**
     * An identification features generator which will compute figures on the
     * identification matches and keep some of them in memory.
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * Object used to monitor the duration of the project creation.
     */
    private Duration projectCreationDuration;
    /**
     * Connection to the database.
     */
    private ObjectsDB objectsDB;
    /**
     * The identification object.
     */
    private Identification identification;
    /**
     * A provider for protein sequences.
     */
    private SequenceProvider sequenceProvider;
    /**
     * A provider for protein details.
     */
    private ProteinDetailsProvider proteinDetailsProvider;
    /**
     * Map of proteins found several times with the number of times they
     * appeared as first hit.
     */
    private HashMap<String, Integer> proteinCount;
    /**
     * The input map.
     */
    private InputMap inputMap;

    /**
     * Empty constructor for instantiation purposes.
     */
    private PeptideShaker() {
    }

    /**
     * Constructor without mass specification. Calculation will be done on new
     * maps which will be retrieved as compomics utilities parameters.
     *
     * @param projectParameters the experiment conducted
     */
    public PeptideShaker(ProjectParameters projectParameters) {

        this.projectParameters = projectParameters;

    }

    /**
     * Imports identification results from result files.
     *
     * @param waitingHandler the handler displaying feedback to the user
     * @param idFiles the files to import
     * @param spectrumFiles the corresponding spectra (can be empty: spectra
     * will not be loaded)
     * @param identificationParameters identification parameters
     * @param projectDetails the project details
     * @param processingPreferences the initial processing preferences
     * @param exceptionHandler the exception handler
     */
    public void importFiles(WaitingHandler waitingHandler, ArrayList<File> idFiles, ArrayList<File> spectrumFiles,
            IdentificationParameters identificationParameters, ProjectDetails projectDetails,
            ProcessingParameters processingPreferences,
            ExceptionHandler exceptionHandler) {

        projectCreationDuration = new Duration();
        projectCreationDuration.start();

        waitingHandler.appendReport("Import process for " + projectParameters.getProjectUniqueName(), true, true);
        waitingHandler.appendReportEndLine();

        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String dbName = projectParameters.getProjectUniqueName() + df.format(projectParameters.getCreationTime()) + ".psDB";

        objectsDB = new ObjectsDB(PeptideShaker.getMatchesFolder().getAbsolutePath(), dbName);
        identification = new Identification(objectsDB);
        identification.addObject(ProjectParameters.key, projectParameters);

        fileImporter = new FileImporter(identification, identificationParameters, processingPreferences, metrics, projectDetails, waitingHandler, exceptionHandler);
        fileImporter.importFiles(idFiles, spectrumFiles);

        geneMaps = fileImporter.getGeneMaps();
        sequenceProvider = fileImporter.getSequenceProvider();
        proteinDetailsProvider = fileImporter.getProteinDetailsProvider();
        inputMap = fileImporter.getInputMap();
        proteinCount = fileImporter.getProteinCount();

    }

    /**
     * Creates a PeptideShaker project
     *
     * @param waitingHandler the handler displaying feedback to the user
     * @param exceptionHandler handler for exceptions
     * @param identificationParameters the identification parameters
     * @param processingParameters the processing preferences
     * @param projectType the project type
     * @param spectrumCountingParameters the spectrum counting preferences
     * @param projectDetails the project details
     *
     * @throws java.lang.InterruptedException exception thrown if a thread gets
     * interrupted
     * @throws java.util.concurrent.TimeoutException exception thrown if a
     * process times out
     */
    public void createProject(IdentificationParameters identificationParameters, ProcessingParameters processingParameters,
            SpectrumCountingParameters spectrumCountingParameters, ProjectDetails projectDetails, ProjectType projectType,
            WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) throws InterruptedException, TimeoutException {

        identification.getObjectsDB().commit();

        identificationFeaturesGenerator = new IdentificationFeaturesGenerator(identification, identificationParameters, sequenceProvider, metrics, spectrumCountingParameters);

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        PsmScoringParameters psmScoringPreferences = identificationParameters.getPsmScoringParameters();
        FastaParameters fastaParameters = identificationParameters.getSearchParameters().getFastaParameters();
        ArrayList<Integer> usedAlgorithms = projectDetails.getIdentificationAlgorithms();

        if (psmScoringPreferences.isScoringNeeded(usedAlgorithms)) {

            waitingHandler.appendReport("Estimating PSM scores.", true, true);

            PsmScorer psmScorer = new PsmScorer(sequenceProvider, fastaParameters);
            psmScorer.estimateIntermediateScores(identification, inputMap, processingParameters, identificationParameters, sequenceProvider, waitingHandler, exceptionHandler);

            if (psmScoringPreferences.isTargetDecoyNeededForPsmScoring(usedAlgorithms)) {

                if (fastaParameters.isTargetDecoy()) {

                    waitingHandler.appendReport("Estimating intermediate scores probabilities.", true, true);
                    psmScorer.estimateIntermediateScoreProbabilities(identification, inputMap, processingParameters, waitingHandler);

                } else {

                    waitingHandler.appendReport("No decoy sequences found. Impossible to estimate intermediate scores probabilities.", true, true);

                }
            }

            waitingHandler.appendReport("Scoring PSMs.", true, true);
            psmScorer.scorePsms(identification, inputMap, processingParameters, identificationParameters, waitingHandler);

        }

        identification.getObjectsDB().commit();
        System.gc();

        if (fastaParameters.isTargetDecoy()) {

            waitingHandler.appendReport("Computing assumptions probabilities.", true, true);

        } else {

            waitingHandler.appendReport("Importing assumptions scores.", true, true);

        }

        inputMap.estimateProbabilities(waitingHandler);
        waitingHandler.increasePrimaryProgressCounter();

        if (waitingHandler.isRunCanceled()) {

            return;

        }

        identification.getObjectsDB().commit();
        System.gc();

        waitingHandler.appendReport("Saving assumptions probabilities, selecting best match, scoring modification localization.", true, true);

        psmProcessing(inputMap, identificationParameters, waitingHandler);
        waitingHandler.increasePrimaryProgressCounter();

        if (waitingHandler.isRunCanceled()) {

            return;

        }

        identification.getObjectsDB().commit();
        System.gc();

        waitingHandler.appendReport("Computing PSM probabilities.", true, true);

        matchesValidator.getPsmMap().estimateProbabilities(waitingHandler);

        if (waitingHandler.isRunCanceled()) {

            return;

        }

        identification.getObjectsDB().commit();
        System.gc();

        if (projectType == ProjectType.peptide || projectType == ProjectType.protein) {

            ModificationLocalizationParameters modificationScoringPreferences = identificationParameters.getModificationLocalizationParameters();

            if (modificationScoringPreferences.getAlignNonConfidentModifications()) {

                waitingHandler.appendReport("Resolving peptide inference issues.", true, true);
                ptmScorer.peptideInference(identification, sequenceProvider, identificationParameters, waitingHandler);

                waitingHandler.increasePrimaryProgressCounter();

                if (waitingHandler.isRunCanceled()) {

                    return;

                }
            }

            identification.getObjectsDB().commit();
            System.gc();

        }

        String reportTxt;
        switch (projectType) {
            case psm:
                reportTxt = "Saving probabilities";
                break;
            case peptide:
                reportTxt = "Saving probabilities, building peptides.";
                break;
            default:
                reportTxt = "Saving probabilities, building peptides and proteins.";
        }

        waitingHandler.appendReport(reportTxt, true, true);
        waitingHandler.setWaitingText(reportTxt + " Please Wait...");

        attachSpectrumProbabilitiesAndBuildPeptidesAndProteins(sequenceProvider, identificationParameters.getSequenceMatchingParameters(), projectType, fastaParameters, waitingHandler);
        waitingHandler.increasePrimaryProgressCounter();

        if (waitingHandler.isRunCanceled()) {

            return;

        }

        identification.getObjectsDB().commit();
        System.gc();

        ProteinInference proteinInference = new ProteinInference();

        if (projectType == ProjectType.protein) {

            if (identificationParameters.getProteinInferenceParameters().getSimplifyGroups()) {

                waitingHandler.appendReport("Simplifying protein groups.", true, true);
                proteinInference.removeRedundantGroups(identification, identificationParameters, sequenceProvider, proteinDetailsProvider, waitingHandler);
                waitingHandler.increasePrimaryProgressCounter();

                if (waitingHandler.isRunCanceled()) {

                    return;

                }
            }

            identification.getObjectsDB().commit();
            System.gc();

        }

        if (projectType == ProjectType.peptide || projectType == ProjectType.protein) {

            waitingHandler.appendReport("Generating peptide map.", true, true);
            matchesValidator.fillPeptideMaps(identification, metrics, waitingHandler, identificationParameters);

            if (waitingHandler.isRunCanceled()) {

                return;

            }

            identification.getObjectsDB().commit();
            System.gc();

            waitingHandler.appendReport("Computing peptide probabilities.", true, true);

            matchesValidator.getPeptideMap().estimateProbabilities(waitingHandler);

            if (waitingHandler.isRunCanceled()) {

                return;

            }

            identification.getObjectsDB().commit();
            System.gc();

            waitingHandler.appendReport("Saving peptide probabilities.", true, true);
            matchesValidator.attachPeptideProbabilities(identification, fastaParameters, waitingHandler);
            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                return;

            }

            identification.getObjectsDB().commit();
            System.gc();

            if (projectType == ProjectType.protein) {

                waitingHandler.appendReport("Generating protein map.", true, true);
                matchesValidator.fillProteinMap(identification, waitingHandler);
                waitingHandler.increasePrimaryProgressCounter();

                if (waitingHandler.isRunCanceled()) {

                    return;

                }

                identification.getObjectsDB().commit();
                System.gc();

                waitingHandler.appendReport("Resolving protein inference issues, inferring peptide and protein PI status.", true, true); // could be slow
                proteinInference.inferPiStatus(identification, metrics, matchesValidator.getProteinMap(), identificationParameters, sequenceProvider, proteinDetailsProvider, waitingHandler);
                waitingHandler.increasePrimaryProgressCounter();

                if (waitingHandler.isRunCanceled()) {

                    return;

                }

                identification.getObjectsDB().commit();
                System.gc();

                waitingHandler.appendReport("Saving protein probabilities.", true, true);
                matchesValidator.attachProteinProbabilities(identification, sequenceProvider, fastaParameters, metrics, waitingHandler, identificationParameters.getFractionParameters());
                waitingHandler.increasePrimaryProgressCounter();

                if (waitingHandler.isRunCanceled()) {

                    return;

                }

                identification.getObjectsDB().commit();
                System.gc();

            }
        }

        if (fastaParameters.isTargetDecoy()) {

            IdMatchValidationParameters idMatchValidationParameters = identificationParameters.getIdValidationParameters();

            if (idMatchValidationParameters.getDefaultPsmFDR() == 1
                    && idMatchValidationParameters.getDefaultPeptideFDR() == 1
                    && idMatchValidationParameters.getDefaultProteinFDR() == 1) {

                waitingHandler.appendReport("Validating identifications at 1% FDR, quality control of matches.", true, true);

            } else {

                waitingHandler.appendReport("Validating identifications, quality control of matches.", true, true);

            }
        } else {

            waitingHandler.appendReport("Quality control of matches.", true, true);

        }

        matchesValidator.validateIdentifications(identification, metrics, inputMap,
                waitingHandler, exceptionHandler,
                identificationFeaturesGenerator, sequenceProvider,
                proteinDetailsProvider, geneMaps,
                identificationParameters, spectrumCountingParameters,
                projectType, processingParameters);
        waitingHandler.increasePrimaryProgressCounter();

        if (waitingHandler.isRunCanceled()) {

            return;

        }

        identification.getObjectsDB().commit();
        System.gc();

        if (projectType == ProjectType.peptide || projectType == ProjectType.protein) {

            waitingHandler.appendReport("Scoring PTMs in peptides.", true, true);
            ptmScorer.scorePeptidePtms(identification, waitingHandler, identificationParameters);
            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                return;

            }

            identification.getObjectsDB().commit();
            System.gc();

            if (projectType == ProjectType.protein) {

                waitingHandler.appendReport("Scoring PTMs in proteins.", true, true);
                ptmScorer.scoreProteinPtms(identification, metrics, waitingHandler, identificationParameters, identificationFeaturesGenerator);
                waitingHandler.increasePrimaryProgressCounter();

                if (waitingHandler.isRunCanceled()) {

                    return;

                }

                identification.getObjectsDB().commit();
                System.gc();

            }
        }

        projectCreationDuration.end();
        String report = "Identification processing completed (" + projectCreationDuration.toString() + ").";

        waitingHandler.appendReport(report, true, true);
        waitingHandler.appendReportEndLine();
        waitingHandler.appendReportEndLine();
        identification.addUrParam(new PSMaps(inputMap, matchesValidator.getPsmMap(), matchesValidator.getPeptideMap(), matchesValidator.getProteinMap()));
        waitingHandler.setRunFinished();

    }

    /**
     * Processes the identifications if a change occurred in the PSM map.
     *
     * @param identification the identification object containing the
     * identification matches
     * @param waitingHandler the waiting handler
     * @param processingPreferences the processing preferences
     * @param identificationParameters the identification parameters
     * @param sequenceProvider a protein sequence provider
     * @param projectType the project type
     */
    public void spectrumMapChanged(Identification identification, WaitingHandler waitingHandler, ProcessingParameters processingPreferences,
            IdentificationParameters identificationParameters, SequenceProvider sequenceProvider, ProjectType projectType) {

        FastaParameters fastaParameters = identificationParameters.getSearchParameters().getFastaParameters();
        FractionParameters fractionParameters = identificationParameters.getFractionParameters();

        TargetDecoyMap peptideMap = new TargetDecoyMap();
        TargetDecoyMap proteinMap = new TargetDecoyMap();
        matchesValidator.setPeptideMap(peptideMap);
        matchesValidator.setProteinMap(proteinMap);
        attachSpectrumProbabilitiesAndBuildPeptidesAndProteins(sequenceProvider, identificationParameters.getSequenceMatchingParameters(),
                projectType, identificationParameters.getSearchParameters().getFastaParameters(), waitingHandler);
        matchesValidator.fillPeptideMaps(identification, metrics, waitingHandler, identificationParameters);
        peptideMap.estimateProbabilities(waitingHandler);
        matchesValidator.attachPeptideProbabilities(identification, fastaParameters, waitingHandler);
        matchesValidator.fillProteinMap(identification, waitingHandler);
        proteinMap.estimateProbabilities(waitingHandler);
        matchesValidator.attachProteinProbabilities(identification, sequenceProvider, fastaParameters, metrics, waitingHandler, fractionParameters);

    }

    /**
     * Processes the identifications if a change occurred in the peptide map.
     *
     * @param identification the identification object containing the
     * identification matches
     * @param waitingHandler the waiting handler
     * @param identificationParameters the identification parameters
     * @param sequenceProvider a protein sequence provider
     */
    public void peptideMapChanged(Identification identification, WaitingHandler waitingHandler,
            IdentificationParameters identificationParameters, SequenceProvider sequenceProvider) {

        FastaParameters fastaParameters = identificationParameters.getSearchParameters().getFastaParameters();
        FractionParameters fractionParameters = identificationParameters.getFractionParameters();

        TargetDecoyMap proteinMap = new TargetDecoyMap();
        matchesValidator.setProteinMap(proteinMap);
        matchesValidator.attachPeptideProbabilities(identification, fastaParameters, waitingHandler);
        matchesValidator.fillProteinMap(identification, waitingHandler);
        proteinMap.estimateProbabilities(waitingHandler);
        matchesValidator.attachProteinProbabilities(identification, sequenceProvider, fastaParameters, metrics, waitingHandler, fractionParameters);

    }

    /**
     * Processes the identifications if a change occurred in the protein map.
     *
     * @param identification the identification object containing the
     * identification matches
     * @param waitingHandler the waiting handler
     * @param identificationParameters the identification parameters
     * @param sequenceProvider a protein sequence provider
     */
    public void proteinMapChanged(Identification identification, WaitingHandler waitingHandler,
            IdentificationParameters identificationParameters, SequenceProvider sequenceProvider) {

        FastaParameters fastaParameters = identificationParameters.getSearchParameters().getFastaParameters();
        FractionParameters fractionParameters = identificationParameters.getFractionParameters();

        matchesValidator.attachProteinProbabilities(identification, sequenceProvider, fastaParameters, metrics, waitingHandler, fractionParameters);

    }

    /**
     * Iterates the spectrum matches and saves assumption probabilities, selects
     * best hits, scores modification localization, and refines protein mapping
     * accordingly.
     *
     * @param inputMap the input map
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler
     */
    private void psmProcessing(InputMap inputMap, IdentificationParameters identificationParameters, WaitingHandler waitingHandler) {

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        FastaParameters fastaParameters = identificationParameters.getSearchParameters().getFastaParameters();
        BestMatchSelection bestMatchSelection = new BestMatchSelection(identification, proteinCount, sequenceProvider, fastaParameters);

        identification.getSpectrumIdentification().values().stream()
                .flatMap(HashSet::parallelStream)
                .map(key -> identification.getSpectrumMatch(key))
                .forEach(spectrumMatch -> psmProcessing(spectrumMatch, inputMap, bestMatchSelection, identificationParameters, waitingHandler));

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

    }

    /**
     * Saves assumption probabilities, selects best hit, scores modification
     * localization, and refines protein mapping accordingly for the given
     * spectrum match.
     *
     * @param spectrumMatch the spectrum match to process
     * @param inputMap the input map
     * @param bestMatchSelection best match selection object
     * @param identificationParameters the identification parameters
     * @param waitingHandler a waiting handler
     */
    private void psmProcessing(SpectrumMatch spectrumMatch, InputMap inputMap,
            BestMatchSelection bestMatchSelection, IdentificationParameters identificationParameters,
            WaitingHandler waitingHandler) {

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        FastaParameters fastaParameters = identificationParameters.getSearchParameters().getFastaParameters();
        SequenceMatchingParameters sequenceMatchingParameters = identificationParameters.getSequenceMatchingParameters();
        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();

        PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();

        attachAssumptionsProbabilities(spectrumMatch, inputMap, fastaParameters, sequenceMatchingParameters, waitingHandler);

        bestMatchSelection.selectBestHit(spectrumMatch, inputMap, waitingHandler, identificationParameters);

        if (spectrumMatch.getBestPeptideAssumption() != null) {

            // Score modification localization
            ModificationLocalizationScorer modificationLocalizationScorer = new ModificationLocalizationScorer();
            modificationLocalizationScorer.scorePTMs(identification, spectrumMatch, sequenceProvider, identificationParameters, waitingHandler, peptideSpectrumAnnotator);

            // Set modification sites
            modificationLocalizationScorer.modificationSiteInference(spectrumMatch, sequenceProvider, identificationParameters);

            // Update protein mapping based on modification profile
            if (identificationParameters.getProteinInferenceParameters().isModificationRefinement()) {

                spectrumMatch.getAllPeptideAssumptions().forEach(
                        peptideAssumption -> PeptideChecker.checkPeptide(peptideAssumption.getPeptide(), sequenceProvider, modificationSequenceMatchingParameters));

            }
        }

        waitingHandler.increaseSecondaryProgressCounter();

    }

    /**
     * Attaches the spectrum posterior error probabilities to the peptide
     * assumptions.
     *
     * @param inputMap map of the input scores
     * @param fastaParameters the fasta parsing parameters
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param waitingHandler the handler displaying feedback to the user
     */
    private void attachAssumptionsProbabilities(SpectrumMatch spectrumMatch, InputMap inputMap, FastaParameters fastaParameters, SequenceMatchingParameters sequenceMatchingPreferences, WaitingHandler waitingHandler) {

        // Peptides
        HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> peptideAssumptionsMap = spectrumMatch.getPeptideAssumptionsMap();
        TreeMap<Double, ArrayList<PSParameter>> pepToParameterMap = new TreeMap<>();

        for (Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> entry : peptideAssumptionsMap.entrySet()) {

            int searchEngine = entry.getKey();
            TreeMap<Double, ArrayList<PeptideAssumption>> seMapping = entry.getValue();
            double previousP = 0;
            ArrayList<PSParameter> previousAssumptionsParameters = new ArrayList<>();
            PeptideAssumption previousAssumption = null;

            for (Entry<Double, ArrayList<PeptideAssumption>> entry2 : seMapping.entrySet()) {

                int eValue = entry.getKey();
                ArrayList<PeptideAssumption> peptideAssumptions = entry2.getValue();

                for (PeptideAssumption assumption : peptideAssumptions) {

                    PSParameter psParameter = (PSParameter) assumption.getUrParam(PSParameter.dummy);

                    if (psParameter == null) {

                        psParameter = new PSParameter();

                    }

                    if (fastaParameters.isTargetDecoy()) {

                        double newP = inputMap.getProbability(searchEngine, eValue);
                        double pep = previousP;

                        if (newP > previousP) {

                            pep = newP;
                            previousP = newP;

                        }

                        psParameter.setProbability(pep);

                        ArrayList<PSParameter> pSParameters = pepToParameterMap.get(pep);

                        if (pSParameters == null) {

                            pSParameters = new ArrayList<>(1);
                            pepToParameterMap.put(pep, pSParameters);

                        }

                        pSParameters.add(psParameter);

                        if (previousAssumption != null) {

                            Peptide newPeptide = assumption.getPeptide();
                            Peptide previousPeptide = previousAssumption.getPeptide();

                            if (!newPeptide.isSameSequenceAndModificationStatus(previousPeptide, sequenceMatchingPreferences)) {

                                for (PSParameter previousParameter : previousAssumptionsParameters) {

                                    double deltaPEP = pep - previousParameter.getProbability();
                                    previousParameter.setAlgorithmDeltaPEP(deltaPEP);

                                }

                                previousAssumptionsParameters.clear();

                            }
                        }

                        previousAssumption = assumption;
                        previousAssumptionsParameters.add(psParameter);

                    } else {

                        psParameter.setProbability(1.0);

                    }

                    assumption.addUrParam(psParameter);

                }
            }

            for (PSParameter previousParameter : previousAssumptionsParameters) {

                double deltaPEP = 1 - previousParameter.getProbability();
                previousParameter.setAlgorithmDeltaPEP(deltaPEP);

            }
        }

        // Compute the delta pep score accross all search engines
        Double previousPEP = null;
        ArrayList<PSParameter> previousParameters = new ArrayList<>();

        for (Entry<Double, ArrayList<PSParameter>> entry : pepToParameterMap.entrySet()) {

            double pep = entry.getKey();

            if (previousPEP != null) {

                for (PSParameter previousParameter : previousParameters) {

                    double delta = pep - previousPEP;
                    previousParameter.setDeltaPEP(delta);

                }
            }

            previousParameters = entry.getValue();
            previousPEP = pep;

        }

        for (PSParameter previousParameter : previousParameters) {

            double delta = 1 - previousParameter.getProbability();
            previousParameter.setDeltaPEP(delta);

        }

        waitingHandler.increaseSecondaryProgressCounter();

        if (waitingHandler.isRunCanceled()) {

            return;

        }

        // Assumptions
        HashMap<Integer, TreeMap<Double, ArrayList<TagAssumption>>> tagAssumptionsMap = spectrumMatch.getTagAssumptionsMap();

        for (Entry<Integer, TreeMap<Double, ArrayList<TagAssumption>>> entry : tagAssumptionsMap.entrySet()) {

            int algorithm = entry.getKey();
            TreeMap<Double, ArrayList<TagAssumption>> seMapping = entry.getValue();
            double previousP = 0;
            ArrayList<PSParameter> previousAssumptionsParameters = new ArrayList<>();
            TagAssumption previousAssumption = null;

            for (Entry<Double, ArrayList<TagAssumption>> entry2 : seMapping.entrySet()) {

                double score = entry2.getKey();

                for (TagAssumption assumption : entry2.getValue()) {

                    PSParameter psParameter = new PSParameter();
                    psParameter = (PSParameter) assumption.getUrParam(psParameter);

                    if (psParameter == null) {

                        psParameter = new PSParameter();

                    }

                    if (fastaParameters.isTargetDecoy()) {

                        double newP = inputMap.getProbability(algorithm, score);
                        double pep = previousP;

                        if (newP > previousP) {

                            pep = newP;
                            previousP = newP;

                        }

                        psParameter.setProbability(pep);

                        ArrayList<PSParameter> pSParameters = pepToParameterMap.get(pep);

                        if (pSParameters == null) {

                            pSParameters = new ArrayList<>(1);
                            pepToParameterMap.put(pep, pSParameters);

                        }

                        pSParameters.add(psParameter);

                        if (previousAssumption != null) {

                            boolean same = false;
                            Tag newTag = ((TagAssumption) assumption).getTag();
                            Tag previousTag = previousAssumption.getTag();

                            if (newTag.isSameSequenceAndModificationStatusAs(previousTag, sequenceMatchingPreferences)) {

                                same = true;

                            }

                            if (!same) {

                                for (PSParameter previousParameter : previousAssumptionsParameters) {

                                    double deltaPEP = pep - previousParameter.getProbability();
                                    previousParameter.setAlgorithmDeltaPEP(deltaPEP);

                                }

                                previousAssumptionsParameters.clear();

                            }
                        }

                        previousAssumption = assumption;
                        previousAssumptionsParameters.add(psParameter);

                    } else {

                        psParameter.setProbability(1.0);

                    }

                    assumption.addUrParam(psParameter);

                }
            }

            for (PSParameter previousParameter : previousAssumptionsParameters) {

                double deltaPEP = 1 - previousParameter.getProbability();
                previousParameter.setAlgorithmDeltaPEP(deltaPEP);

            }
        }
    }

    /**
     * Attaches the spectrum posterior error probabilities to the spectrum
     * matches.
     *
     * @param sequenceProvider a protein sequence provider
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param projectType the project type
     * @param fastaParameters the fasta parsing parameters
     * @param waitingHandler the handler displaying feedback to the user
     */
    private void attachSpectrumProbabilitiesAndBuildPeptidesAndProteins(
            SequenceProvider sequenceProvider, SequenceMatchingParameters sequenceMatchingPreferences,
            ProjectType projectType, FastaParameters fastaParameters, WaitingHandler waitingHandler) {

        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());

        PSParameter psParameter = new PSParameter();

        SpectrumMatchesIterator psmIterator = identification.getSpectrumMatchesIterator(waitingHandler);

        SpectrumMatch spectrumMatch;
        while ((spectrumMatch = psmIterator.next()) != null) {

            psParameter = (PSParameter) spectrumMatch.getUrParam(psParameter);

            if (spectrumMatch.getBestPeptideAssumption() == null) {
                continue;
            }

            if (fastaParameters.isTargetDecoy()) {

                psParameter.setProbability(matchesValidator.getPsmMap().getProbability(psParameter.getScore()));

            } else {

                psParameter.setProbability(1.0);

            }

            if (projectType == ProjectType.peptide || projectType == ProjectType.protein) {

                identification.buildPeptidesAndProteins(spectrumMatch, sequenceMatchingPreferences, sequenceProvider, projectType == ProjectType.protein);

            }

            waitingHandler.increaseSecondaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {

                return;

            }
        }

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
    }

    /**
     * Returns the metrics picked-up while loading the files.
     *
     * @return the metrics picked-up while loading the files
     */
    public Metrics getMetrics() {

        return metrics;

    }

    /**
     * Returns the gene maps.
     *
     * @return the gene maps
     */
    public GeneMaps getGeneMaps() {

        return geneMaps;

    }

    /**
     * Returns the identification object.
     *
     * @return the identification object
     */
    public Identification getIdentification() {

        return identification;

    }

    /**
     * Sets the gene maps.
     *
     * @param geneMaps the new gene maps
     */
    public void setGeneMaps(GeneMaps geneMaps) {

        this.geneMaps = geneMaps;

    }

    /**
     * Returns the identification features generator used when loading the
     * files.
     *
     * @return the identification features generator used when loading the files
     */
    public IdentificationFeaturesGenerator getIdentificationFeaturesGenerator() {

        return identificationFeaturesGenerator;

    }

    /**
     * Verifies that the modifications backed-up in the search parameters are
     * loaded and returns an error message if one was already loaded, null
     * otherwise.
     *
     * @param searchParameters the search parameters to load
     * @return an error message if one was already loaded, null otherwise
     */
    public static String loadModifications(SearchParameters searchParameters) {

        String error = null;
        ArrayList<String> toCheck = ModificationFactory.getInstance().loadBackedUpModifications(searchParameters, true);

        if (!toCheck.isEmpty()) {

            error = "The definition of the following PTM(s) seems to have changed and were overwritten:\n";

            for (int i = 0; i < toCheck.size(); i++) {

                if (i > 0) {

                    if (i < toCheck.size() - 1) {

                        error += ", ";

                    } else {

                        error += " and ";

                    }
                }

                error += toCheck.get(i);

            }

            error += ".\nPlease verify the definition of the PTM(s) in the modifications editor.";

        }

        return error;

    }

    /**
     * Returns the file used for user preferences storage.
     *
     * @return the file used for user preferences storage
     */
    public static String getUserPreferencesFile() {

        return USER_PREFERENCES_FILE;

    }

    /**
     * Returns the folder used for user preferences storage.
     *
     * @return the folder used for user preferences storage
     */
    public static String getUserPreferencesFolder() {

        File tempFile = new File(getUserPreferencesFile());
        return tempFile.getParent();

    }

    /**
     * Sets the file used for user preferences storage.
     *
     * @param userPreferencesFolder the folder used for user preferences storage
     */
    public static void setUserPreferencesFolder(String userPreferencesFolder) {

        File tempFile = new File(userPreferencesFolder, "userpreferences.cpf");
        PeptideShaker.USER_PREFERENCES_FILE = tempFile.getAbsolutePath();

    }

    /**
     * Returns the directory used to store the identification matches.
     *
     * @return the directory used to store the identification matches
     */
    public static String getMatchesDirectorySubPath() {

        return DATABASE_DIRECTORY;

    }

    /**
     * Returns the matches directory parent. An empty string if not set. Can be
     * a relative path.
     *
     * @return the matches directory parent
     */
    public static String getMatchesDirectoryParent() {

        return SERIALIZATION_PARENT_DIRECTORY;

    }

    /**
     * Returns the matches directory parent. An empty string if not set.
     *
     * @return the matches directory parent
     */
    public static File getMatchesDirectoryParentFile() {
        String matchesParentDirectory = PeptideShaker.getMatchesDirectoryParent();

        return matchesParentDirectory.equals("resources")
                ? new File(getJarFilePath(), matchesParentDirectory) : new File(matchesParentDirectory);

    }

    /**
     * Sets the matches directory parent.
     *
     * @param matchesDirectoryParent the matches directory parent
     * @throws IOException thrown of an exception occurs
     */
    public static void setMatchesDirectoryParent(String matchesDirectoryParent) throws IOException {

        PeptideShaker.SERIALIZATION_PARENT_DIRECTORY = matchesDirectoryParent;
        File serializationFolder = new File(matchesDirectoryParent, PeptideShaker.getMatchesDirectorySubPath());

        if (!serializationFolder.exists()) {

            serializationFolder.mkdirs();

            if (!serializationFolder.exists()) {

                throw new IOException("Impossible to create folder " + serializationFolder.getAbsolutePath() + ".");

            }
        }
    }

    /**
     * Returns the path to the matches folder according to the user path
     * settings.
     *
     * @return the path to the match folder according to the user path settings
     */
    public static File getMatchesFolder() {

        return new File(getMatchesDirectoryParentFile(), PeptideShaker.getMatchesDirectorySubPath());

    }

    /**
     * Instantiates the spectrum, sequence, and PTM factories with caches
     * adapted to the memory available as set in the user preferences.
     *
     * @param utilitiesUserPreferences the user preferences
     */
    public static void instantiateFacories(UtilitiesUserParameters utilitiesUserPreferences) {

        int nSpectra;

        if (utilitiesUserPreferences.getMemoryParameter() > 32000) {

            nSpectra = 100000000;

        } else if (utilitiesUserPreferences.getMemoryParameter() > 16000) {

            nSpectra = 50000000;

        } else if (utilitiesUserPreferences.getMemoryParameter() > 8000) {

            nSpectra = 10000000;

        } else if (utilitiesUserPreferences.getMemoryParameter() > 4000) {

            nSpectra = 5000000;

        } else {

            nSpectra = 1000000;

        }

        EnzymeFactory.getInstance();
        ModificationFactory.getInstance();
        SpectrumFactory.getInstance(nSpectra);

    }

    /**
     * Retrieves the version number set in the pom file.
     *
     * @return the version number of PeptideShaker
     */
    public static String getVersion() {

        java.util.Properties p = new java.util.Properties();

        try {

            InputStream is = (new PeptideShaker()).getClass().getClassLoader().getResourceAsStream("peptide-shaker.properties");
            p.load(is);

        } catch (IOException e) {

            e.printStackTrace();

        }

        return p.getProperty("peptide-shaker.version");

    }

    /**
     * Retrieves the version number set in the pom file.
     *
     * @return the version number of PeptideShaker
     */
    public static String getJarFilePath() {

        return CompomicsWrapper.getJarFilePath((new PeptideShaker()).getClass().getResource("PeptideShaker.class").getPath(), "PeptideShaker");

    }
}
