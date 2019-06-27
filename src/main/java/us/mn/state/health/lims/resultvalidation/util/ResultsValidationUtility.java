/**
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) CIRG, University of Washington, Seattle WA.  All Rights Reserved.
 *               I-TECH, University of Washington, Seattle WA.
 */
package us.mn.state.health.lims.resultvalidation.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.validator.GenericValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import spring.mine.internationalization.MessageUtil;
import spring.service.analysis.AnalysisService;
import spring.service.analyte.AnalyteService;
import spring.service.dictionary.DictionaryService;
import spring.service.note.NoteService;
import spring.service.note.NoteServiceImpl.NoteType;
import spring.service.observationhistory.ObservationHistoryService;
import spring.service.observationhistorytype.ObservationHistoryTypeService;
import spring.service.result.ResultService;
import spring.service.sample.SampleService;
import spring.service.test.TestSectionService;
import spring.service.test.TestService;
import spring.service.test.TestServiceImpl;
import spring.service.testresult.TestResultService;
import spring.service.typeoftestresult.TypeOfTestResultServiceImpl;
import spring.util.SpringContext;
import us.mn.state.health.lims.analysis.valueholder.Analysis;
import us.mn.state.health.lims.analyte.valueholder.Analyte;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.common.services.QAService;
import us.mn.state.health.lims.common.services.StatusService;
import us.mn.state.health.lims.common.services.StatusService.AnalysisStatus;
import us.mn.state.health.lims.common.services.StatusService.RecordStatus;
import us.mn.state.health.lims.common.services.TestIdentityService;
import us.mn.state.health.lims.common.util.IdValuePair;
import us.mn.state.health.lims.common.util.StringUtil;
import us.mn.state.health.lims.dictionary.valueholder.Dictionary;
import us.mn.state.health.lims.observationhistory.valueholder.ObservationHistory;
import us.mn.state.health.lims.observationhistorytype.valueholder.ObservationHistoryType;
import us.mn.state.health.lims.result.valueholder.Result;
import us.mn.state.health.lims.resultvalidation.action.util.ResultValidationItem;
import us.mn.state.health.lims.resultvalidation.bean.AnalysisItem;
import us.mn.state.health.lims.sample.valueholder.Sample;
import us.mn.state.health.lims.statusofsample.util.StatusRules;
import us.mn.state.health.lims.test.valueholder.Test;
import us.mn.state.health.lims.testresult.valueholder.TestResult;

@Service
public class ResultsValidationUtility {

	@Autowired
	protected DictionaryService dictionaryService;
	@Autowired
	protected TestSectionService testSectionService;
	@Autowired
	protected ResultService resultService;
	@Autowired
	protected TestResultService testResultService;
	@Autowired
	protected TestService testService;
	@Autowired
	protected SampleService sampleService;
	@Autowired
	protected ObservationHistoryService observationHistoryService;
	@Autowired
	protected AnalyteService analyteService;
	@Autowired
	protected ObservationHistoryTypeService ohTypeService;
	@Autowired
	protected AnalysisService analysisService;

	protected String SAMPLE_STATUS_OBSERVATION_HISTORY_TYPE_ID;
	protected String CD4_COUNT_SORT_NUMBER;

	protected String ANALYTE_CD4_CT_GENERATED_ID;
	protected String CONCLUSION_ID;

	protected List<Integer> notValidStatus = new ArrayList<>();
	protected Map<String, String> testIdToUnits = new HashMap<>();
	protected Map<String, Boolean> accessionToValidMap;
	protected String totalTestName = "";

	@PostConstruct
	private void initilaizeGlobalVariables() {
		notValidStatus.add(Integer.parseInt(StatusService.getInstance().getStatusID(AnalysisStatus.Finalized)));
		notValidStatus.add(Integer.parseInt(StatusService.getInstance().getStatusID(AnalysisStatus.TechnicalRejected)));
		Analyte analyte = new Analyte();
		analyte.setAnalyteName("Conclusion");
		analyte = analyteService.getAnalyteByName(analyte, false);
		CONCLUSION_ID = analyte.getId();
		analyte = new Analyte();
		analyte.setAnalyteName("generated CD4 Count");
		analyte = analyteService.getAnalyteByName(analyte, false);
		ANALYTE_CD4_CT_GENERATED_ID = analyte == null ? "" : analyte.getId();

		Test test = testService.getTestByName("CD4 absolute count");
		if (test != null) {
			CD4_COUNT_SORT_NUMBER = test.getSortOrder();
		}

		ObservationHistoryType oht = ohTypeService.getByName("SampleRecordStatus");
		if (oht != null) {
			SAMPLE_STATUS_OBSERVATION_HISTORY_TYPE_ID = oht.getId();
		}
	}

	public List<AnalysisItem> getResultValidationList(List<Integer> statusList, String testSectionId) {

		List<AnalysisItem> resultList = new ArrayList<>();

		if (!GenericValidator.isBlankOrNull(testSectionId)) {
			List<ResultValidationItem> testList = getUnValidatedTestResultItemsInTestSection(testSectionId, statusList);
			resultList = testResultListToAnalysisItemList(testList);
			sortByAccessionNumberAndOrder(resultList);
			setGroupingNumbers(resultList);
		}

		return resultList;

	}

	@SuppressWarnings("unchecked")
	public final List<ResultValidationItem> getUnValidatedTestResultItemsInTestSection(String sectionId,
			List<Integer> statusList) {

		List<Analysis> analysisList = analysisService.getAllAnalysisByTestSectionAndStatus(sectionId, statusList,
				false);
		return getGroupedTestsForAnalysisList(analysisList, !StatusRules.useRecordStatusForValidation());
	}

	protected final void sortByAccessionNumberAndOrder(List<AnalysisItem> resultItemList) {
		Collections.sort(resultItemList, new Comparator<AnalysisItem>() {
			@Override
			public final int compare(AnalysisItem a, AnalysisItem b) {
				int accessionComp = a.getAccessionNumber().compareTo(b.getAccessionNumber());
				return ((accessionComp == 0)
						? Integer.parseInt(a.getTestSortNumber()) - Integer.parseInt(b.getTestSortNumber())
						: accessionComp);
			}
		});

	}

	protected final void setGroupingNumbers(List<AnalysisItem> resultList) {
		String currentAccessionNumber = null;
		AnalysisItem headItem = null;
		int groupingCount = 1;

		for (AnalysisItem analysisResultItem : resultList) {
			if (!analysisResultItem.getAccessionNumber().equals(currentAccessionNumber)) {
				currentAccessionNumber = analysisResultItem.getAccessionNumber();
				headItem = analysisResultItem;
				groupingCount++;
			} else {
				headItem.setMultipleResultForSample(true);
				analysisResultItem.setMultipleResultForSample(true);
			}

			analysisResultItem.setSampleGroupingNumber(groupingCount);
		}
	}

	/*
	 * N.B. The ignoreRecordStatus is an abomination and should be removed. It is a
	 * quick and dirty fix for workplan and validation using the same code but
	 * having different rules
	 */
	public final List<ResultValidationItem> getGroupedTestsForAnalysisList(Collection<Analysis> filteredAnalysisList,
			boolean ignoreRecordStatus) throws LIMSRuntimeException {

		List<ResultValidationItem> selectedTestList = new ArrayList<>();
		Dictionary dictionary;

		for (Analysis analysis : filteredAnalysisList) {

			if (ignoreRecordStatus || sampleReadyForValidation(analysis.getSampleItem().getSample())) {
				List<ResultValidationItem> testResultItemList = getResultItemFromAnalysis(analysis);
				// NB. The resultValue is filled in during getResultItemFromAnalysis as a side
				// effect of setResult
				for (ResultValidationItem validationItem : testResultItemList) {
					if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(validationItem.getResultType())) {
						dictionary = new Dictionary();
						String resultValue = null;
						try {
							dictionary.setId(validationItem.getResultValue());
							dictionaryService.getData(dictionary);
							resultValue = GenericValidator.isBlankOrNull(dictionary.getLocalAbbreviation())
									? dictionary.getDictEntry()
									: dictionary.getLocalAbbreviation();
						} catch (Exception e) {
							// no-op
						}

						validationItem.setResultValue(resultValue);

					}

					validationItem.setAnalysis(analysis);
					validationItem.setNonconforming(QAService.isAnalysisParentNonConforming(analysis) || StatusService
							.getInstance().matches(analysis.getStatusId(), AnalysisStatus.TechnicalRejected));
					selectedTestList.add(validationItem);
				}
			}
		}

		return selectedTestList;
	}

	protected final boolean sampleReadyForValidation(Sample sample) {

		Boolean valid = accessionToValidMap.get(sample.getAccessionNumber());

		if (valid == null) {
			valid = getSampleRecordStatus(sample) != RecordStatus.NotRegistered;
			accessionToValidMap.put(sample.getAccessionNumber(), valid);
		}

		return valid;
	}

	public final List<ResultValidationItem> getResultItemFromAnalysis(Analysis analysis) throws LIMSRuntimeException {
		List<ResultValidationItem> testResultList = new ArrayList<>();

		List<Result> resultList = resultService.getResultsByAnalysis(analysis);
		NoteType[] noteTypes = { NoteType.EXTERNAL, NoteType.INTERNAL, NoteType.REJECTION_REASON,
				NoteType.NON_CONFORMITY };
		NoteService noteAnalysisService = SpringContext.getBean(NoteService.class);
		noteAnalysisService.setAnalysis(analysis);
		String notes = noteAnalysisService.getNotesAsString(true, true, "<br/>", noteTypes, false);

		if (resultList == null) {
			return testResultList;
		}

		// For historical reasons we add a null member to the collection if it
		// is empty
		// this should be refactored.
		// The result list are results associated with the analysis, if there is
		// none we want
		// to present the user with a blank one
		if (resultList.isEmpty()) {
			resultList.add(null);
		}

		ResultValidationItem parentItem = null;
		for (Result result : resultList) {
			if (parentItem != null && result.getParentResult() != null
					&& parentItem.getResultId().equals(result.getParentResult().getId())) {
				parentItem.setQualifiedResultValue(result.getValue());
				parentItem.setHasQualifiedResult(true);
				parentItem.setQualificationResultId(result.getId());
				continue;
			}

			ResultValidationItem resultItem = createTestResultItem(analysis, analysis.getTest(),
					analysis.getSampleItem().getSortOrder(), result,
					analysis.getSampleItem().getSample().getAccessionNumber(), notes);

			notes = null;// we only want it once
			if (resultItem.getQualifiedDictionaryId() != null) {
				parentItem = resultItem;
			}

			testResultList.add(resultItem);
		}

		return testResultList;
	}

	protected final ResultValidationItem createTestResultItem(Analysis analysis, Test test, String sequenceNumber,
			Result result, String accessionNumber, String notes) {

		List<TestResult> testResults = getPossibleResultsForTest(test);

		String displayTestName = TestServiceImpl.getLocalizedTestNameWithType(test);
//		displayTestName = augmentTestNameWithRange(displayTestName, result);

		ResultValidationItem testItem = new ResultValidationItem();

		testItem.setAccessionNumber(accessionNumber);
		testItem.setAnalysis(analysis);
		testItem.setSequenceNumber(sequenceNumber);
		testItem.setTestName(displayTestName);
		testItem.setTestId(test.getId());
		testItem.setAnalysisMethod(analysis.getAnalysisType());
		testItem.setResult(result);
		testItem.setDictionaryResults(getAnyDictonaryValues(testResults));
		testItem.setResultType(getTestResultType(testResults));
		testItem.setTestSortNumber(test.getSortOrder());
		testItem.setReflexGroup(analysis.getTriggeredReflex());
		testItem.setChildReflex(analysis.getTriggeredReflex() && isConclusion(result, analysis));
		testItem.setQualifiedDictionaryId(getQualifiedDictionaryId(testResults));
		testItem.setPastNotes(notes);

		return testItem;
	}

	protected final String getQualifiedDictionaryId(List<TestResult> testResults) {
		String qualDictionaryIds = "";
		for (TestResult testResult : testResults) {
			if (testResult.getIsQuantifiable()) {
				if (!"".equals(qualDictionaryIds)) {
					qualDictionaryIds += ",";
				}
				qualDictionaryIds += testResult.getValue();
			}
		}
		return "".equals(qualDictionaryIds) ? null : "[" + qualDictionaryIds + "]";
	}

	protected final String augmentUOMWithRange(String uom, Result result) {
		if (result == null) {
			return uom;
		}
		ResultService resultResultService = SpringContext.getBean(ResultService.class);
		resultResultService.setResult(result);
		String range = resultResultService.getDisplayReferenceRange(true);
		uom = StringUtil.blankIfNull(uom);
		return GenericValidator.isBlankOrNull(range) ? uom : (uom + " ( " + range + " )");
	}

	protected final boolean isConclusion(Result testResult, Analysis analysis) {
		List<Result> results = resultService.getResultsByAnalysis(analysis);
		if (results.size() == 1) {
			return false;
		}

		Long testResultId = Long.parseLong(testResult.getId());
		// This based on the fact that the conclusion is always added
		// after the shared result so if there is a result with a larger id
		// then this is not a conclusion
		for (Result result : results) {
			if (Long.parseLong(result.getId()) > testResultId) {
				return false;
			}
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	protected final List<TestResult> getPossibleResultsForTest(Test test) {
		return testResultService.getAllActiveTestResultsPerTest(test);
	}

	protected final List<IdValuePair> getAnyDictonaryValues(List<TestResult> testResults) {
		List<IdValuePair> values = null;
		Dictionary dictionary;

		if (testResults != null && testResults.size() > 0
				&& TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResults.get(0).getTestResultType())) {
			values = new ArrayList<>();
			values.add(new IdValuePair("0", ""));

			for (TestResult testResult : testResults) {
				// Note: result group use to be a criteria but was removed, if
				// results are not as expected investigate
				if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResult.getTestResultType())) {
					dictionary = dictionaryService.getDataForId(testResult.getValue());
					String displayValue = dictionary.getLocalizedName();

					if ("unknown".equals(displayValue)) {
						displayValue = GenericValidator.isBlankOrNull(dictionary.getLocalAbbreviation())
								? dictionary.getDictEntry()
								: dictionary.getLocalAbbreviation();
					}
					values.add(new IdValuePair(testResult.getValue(), displayValue));
				}
			}
		}

		return values;
	}

	protected final String getTestResultType(List<TestResult> testResults) {
		String testResultType = TypeOfTestResultServiceImpl.ResultType.NUMERIC.getCharacterValue();

		if (testResults != null && testResults.size() > 0) {
			testResultType = testResults.get(0).getTestResultType();
		}

		return testResultType;
	}

	public final List<AnalysisItem> testResultListToAnalysisItemList(List<ResultValidationItem> testResultList) {
		List<AnalysisItem> analysisResultList = new ArrayList<>();

		/*
		 * The issue with multiselect results is that each selection is one
		 * ResultValidationItem but they all need to be condensed into one AnalysisItem.
		 * There is a many to one mapping. The first multiselect result we have gets
		 * rolled into one AnalysisItem and the rest are skipped but we want to capture
		 * any qualified results
		 */
		boolean multiResultEntered = false;
		String currentAccession = null;
		AnalysisItem currentMultiSelectAnalysisItem = null;
		for (ResultValidationItem testResultItem : testResultList) {
			if (!testResultItem.getAccessionNumber().equals(currentAccession)) {
				currentAccession = testResultItem.getAccessionNumber();
				currentMultiSelectAnalysisItem = null;
				multiResultEntered = false;
			}
			if (!multiResultEntered) {
				AnalysisItem convertedItem = testResultItemToAnalysisItem(testResultItem);
				analysisResultList.add(convertedItem);
				if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(testResultItem.getResultType())) {
					multiResultEntered = true;
					currentMultiSelectAnalysisItem = convertedItem;
				}
			}
			if (currentMultiSelectAnalysisItem != null && testResultItem.isHasQualifiedResult()) {
				currentMultiSelectAnalysisItem.setQualifiedResultValue(testResultItem.getQualifiedResultValue());
				currentMultiSelectAnalysisItem.setQualifiedDictionaryId(testResultItem.getQualifiedDictionaryId());
				currentMultiSelectAnalysisItem.setHasQualifiedResult(true);
			}
		}

		return analysisResultList;
	}

	protected final RecordStatus getSampleRecordStatus(Sample sample) {

		List<ObservationHistory> ohList = observationHistoryService.getAll(null, sample,
				SAMPLE_STATUS_OBSERVATION_HISTORY_TYPE_ID);

		if (ohList.isEmpty()) {
			return null;
		}

		return StatusService.getInstance().getRecordStatusForID(ohList.get(0).getValue());
	}

	public final AnalysisItem testResultItemToAnalysisItem(ResultValidationItem testResultItem) {
		AnalysisItem analysisResultItem = new AnalysisItem();
		String testUnits = getUnitsByTestId(testResultItem.getTestId());
		String testName = testResultItem.getTestName();
		String sortOrder = testResultItem.getTestSortNumber();
		Result result = testResultItem.getResult();

		if (result != null && result.getAnalyte() != null
				&& ANALYTE_CD4_CT_GENERATED_ID.equals(testResultItem.getResult().getAnalyte().getId())) {
			testUnits = "";
			testName = MessageUtil.getMessage("result.conclusion.cd4");
			analysisResultItem.setShowAcceptReject(false);
			sortOrder = CD4_COUNT_SORT_NUMBER;
		} else if (testResultItem.getTestName().equals(totalTestName)) {
			analysisResultItem.setShowAcceptReject(false);
			analysisResultItem.setReadOnly(true);
			testUnits = testResultItem.getUnitsOfMeasure();
			analysisResultItem.setIsHighlighted(!"100.0".equals(testResultItem.getResult().getValue()));
		}

		testUnits = augmentUOMWithRange(testUnits, testResultItem.getResult());

		analysisResultItem.setAccessionNumber(testResultItem.getAccessionNumber());
		analysisResultItem.setTestName(testName);
		analysisResultItem.setUnits(testUnits);
		analysisResultItem.setAnalysisId(testResultItem.getAnalysis().getId());
		analysisResultItem.setPastNotes(testResultItem.getPastNotes());
		analysisResultItem.setResultId(testResultItem.getResultId());
		analysisResultItem.setResultType(testResultItem.getResultType());
		analysisResultItem.setTestId(testResultItem.getTestId());
		analysisResultItem.setTestSortNumber(sortOrder);
		analysisResultItem.setDictionaryResults(testResultItem.getDictionaryResults());
		analysisResultItem.setDisplayResultAsLog(
				TestIdentityService.getInstance().isTestNumericViralLoad(testResultItem.getTestId()));
		if (result != null) {
			if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(testResultItem.getResultType())) {
				AnalysisService analysisAnalysisService = SpringContext.getBean(AnalysisService.class);
				analysisAnalysisService.setAnalysis(testResultItem.getAnalysis());
				analysisResultItem.setMultiSelectResultValues(analysisAnalysisService.getJSONMultiSelectResults());
			} else {
				analysisResultItem.setResult(getFormattedResult(testResultItem));
			}

			if (TypeOfTestResultServiceImpl.ResultType.NUMERIC.matches(testResultItem.getResultType())) {
				// analysisResultItem.setSignificantDigits( result.getMinNormal().equals(
				// result.getMaxNormal())? -1 : result.getSignificantDigits());
				analysisResultItem.setSignificantDigits(result.getSignificantDigits());
			}
		}
		analysisResultItem.setReflexGroup(testResultItem.isReflexGroup());
		analysisResultItem.setChildReflex(testResultItem.isChildReflex());
		analysisResultItem.setNonconforming(testResultItem.isNonconforming() || StatusService.getInstance()
				.matches(testResultItem.getAnalysis().getStatusId(), AnalysisStatus.TechnicalRejected));
		analysisResultItem.setQualifiedDictionaryId(testResultItem.getQualifiedDictionaryId());
		analysisResultItem.setQualifiedResultValue(testResultItem.getQualifiedResultValue());
		analysisResultItem.setQualifiedResultId(testResultItem.getQualificationResultId());
		analysisResultItem.setHasQualifiedResult(testResultItem.isHasQualifiedResult());

		return analysisResultItem;

	}

	protected final String getFormattedResult(ResultValidationItem testResultItem) {
		String result = testResultItem.getResult().getValue();
		if (TestIdentityService.getInstance().isTestNumericViralLoad(testResultItem.getTestId())
				&& !GenericValidator.isBlankOrNull(result)) {
			return result.split("\\(")[0].trim();
		} else {
			ResultService resultResultService = SpringContext.getBean(ResultService.class);
			resultResultService.setResult(testResultItem.getResult());
			return resultResultService.getResultValue(false);
		}
	}

	public final String getUnitsByTestId(String testId) {

		String uomName = null;

		if (testId != null) {
			uomName = testIdToUnits.get(testId);
			if (uomName == null) {
				Test test = new Test();
				test.setId(testId);
				test = testService.getTestById(test);

				if (test.getUnitOfMeasure() != null) {
					uomName = test.getUnitOfMeasure().getName();
					testIdToUnits.put(testId, uomName);
				} else {
					testIdToUnits.put(testId, "");
				}
			}
		}
		return uomName;

	}

	protected final String getTestId(String testName) {
		Test test = testService.getTestByName(testName);
		return test.getId();
	}

}
