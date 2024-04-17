import React, { useContext, useState, useEffect } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import "../Style.css";
import { getFromOpenElisServer } from "../utils/Utils";
import {
  Form,
  TextInput,
  Button,
  Grid,
  Column,
  DatePicker,
  DatePickerInput,
  RadioButton,
  RadioButtonGroup,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Pagination,
  Loading,
} from "@carbon/react";
import CustomLabNumberInput from "../common/CustomLabNumberInput";
import { patientSearchHeaderData } from "../data/PatientResultsTableHeaders";
import { Formik, Field } from "formik";
import SearchPatientFormValues from "../formModel/innitialValues/SearchPatientFormValues";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";

function SearchPatientForm(props) {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const [dob, setDob] = useState("");
  const [patientSearchResults, setPatientSearchResults] = useState([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [loading, setLoading] = useState(false);
  const [nextPage, setNextPage] = useState(null);
  const [previousPage, setPreviousPage] = useState(null);
  const [pagination, setPagination] = useState(false);
  const [url, setUrl] = useState("");
  const [searchFormValues, setSearchFormValues] = useState(
    SearchPatientFormValues,
  );
  const handleSubmit = (values) => {
    setLoading(true);
    values.dateOfBirth = dob;
    const searchEndPoint =
      "/rest/patient-search-results?" +
      "lastName=" +
      values.lastName +
      "&firstName=" +
      values.firstName +
      "&STNumber=" +
      values.patientId +
      "&subjectNumber=" +
      values.patientId +
      "&nationalID=" +
      values.patientId +
      "&labNumber=" +
      values.labNumber +
      "&guid=" +
      values.guid +
      "&dateOfBirth=" +
      values.dateOfBirth +
      "&gender=" +
      values.gender;
    getFromOpenElisServer(searchEndPoint, fetchPatientResults);
    setUrl(searchEndPoint);
  };
  const loadNextResultsPage = () => {
    setLoading(true);
    getFromOpenElisServer(url + "&page=" + nextPage, fetchPatientResults);
  };

  const loadPreviousResultsPage = () => {
    setLoading(true);
    getFromOpenElisServer(url + "&page=" + previousPage, fetchPatientResults);
  };

  const fetchPatientResults = (res) => {
    let patientsResults = res.patientSearchResults;
    if (patientsResults.length > 0) {
      patientsResults.forEach((item) => (item.id = item.patientID));
      setPatientSearchResults(patientsResults);
    } else {
      setPatientSearchResults([]);
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "patient.search.nopatient" }),
        kind: NotificationKinds.warning,
      });
      setNotificationVisible(true);
    }
    if (res.paging) {
      var { totalPages, currentPage } = res.paging;
      if (totalPages > 1) {
        setPagination(true);
        if (parseInt(currentPage) < parseInt(totalPages)) {
          setNextPage(parseInt(currentPage) + 1);
        } else {
          setNextPage(null);
        }
        if (parseInt(currentPage) > 1) {
          setPreviousPage(parseInt(currentPage) - 1);
        } else {
          setPreviousPage(null);
        }
      }
    }
    setLoading(false);
  };

  const fetchPatientDetails = (patientDetails) => {
    props.getSelectedPatient(patientDetails);
  };

  const handleDatePickerChange = (...e) => {
    setDob(e[1]);
  };

  const patientSelected = (e) => {
    const patientSelected = patientSearchResults.find((patient) => {
      return patient.patientID == e.target.id;
    });
    const searchEndPoint =
      "/rest/patient-details?patientID=" + patientSelected.patientID;
    getFromOpenElisServer(searchEndPoint, fetchPatientDetails);
  };

  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }

    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  };
  useEffect(() => {
    let patientId = new URLSearchParams(window.location.search).get(
      "patientId",
    );
    if (patientId) {
      const searchEndPoint = "/rest/patient-details?patientID=" + patientId;
      getFromOpenElisServer(searchEndPoint, fetchPatientDetails);
    }
  }, []);
  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading />}
      <Formik
        initialValues={searchFormValues}
        enableReinitialize={true}
        // validationSchema={}
        onSubmit={handleSubmit}
        onChange
      >
        {({
          values,
          //errors,
          //touched,
          setFieldValue,
          handleChange,
          handleBlur,
          handleSubmit,
        }) => (
          <Form
            onSubmit={handleSubmit}
            onChange={handleChange}
            onBlur={handleBlur}
          >
            <Grid>
              <Field name="guid">
                {({ field }) => (
                  <input type="hidden" name={field.name} id={field.name} />
                )}
              </Field>
              <Column lg={16}>
                {" "}
                <br />{" "}
              </Column>
              <Column lg={8}>
                <Field name="patientId">
                  {({ field }) => (
                    <TextInput
                      name={field.name}
                      value={values[field.name]}
                      placeholder={intl.formatMessage({
                        id: "input.placeholder.patientId",
                      })}
                      labelText={intl.formatMessage({
                        id: "patient.id",
                        defaultMessage: "Patient Id",
                      })}
                      id={field.name}
                    />
                  )}
                </Field>
              </Column>
              <Column lg={8}>
                <Field name="labNumber">
                  {({ field }) => (
                    <CustomLabNumberInput
                      name={field.name}
                      placeholder={intl.formatMessage({
                        id: "input.placeholder.prevLabNumber",
                      })}
                      labelText={intl.formatMessage({
                        id: "patient.prev.lab.no",
                        defaultMessage: "Previous Lab Number",
                      })}
                      id={field.name}
                      value={values[field.name]}
                      onChange={(e, rawValue) => {
                        setFieldValue(field.name, rawValue);
                      }}
                    />
                  )}
                </Field>
              </Column>
              <Column lg={16}>
                {" "}
                <br />{" "}
              </Column>
              <Column lg={8}>
                <Field name="lastName">
                  {({ field }) => (
                    <TextInput
                      name={field.name}
                      placeholder={intl.formatMessage({
                        id: "input.placeholder.patientLastName",
                      })}
                      labelText={intl.formatMessage({
                        id: "patient.last.name",
                        defaultMessage: "Last Name",
                      })}
                      id={field.name}
                    />
                  )}
                </Field>
              </Column>
              <Column lg={8}>
                <Field name="firstName">
                  {({ field }) => (
                    <TextInput
                      name={field.name}
                      placeholder={intl.formatMessage({
                        id: "input.placeholder.patientFirstName",
                      })}
                      labelText={intl.formatMessage({
                        id: "patient.first.name",
                        defaultMessage: "First Name",
                      })}
                      id={field.name}
                    />
                  )}
                </Field>
              </Column>
              <Column lg={16}>
                {" "}
                <br />{" "}
              </Column>
              <Column lg={8}>
                <Field name="dateOfBirth">
                  {({ field }) => (
                    <DatePicker
                      onChange={handleDatePickerChange}
                      name={field.name}
                      dateFormat="d/m/Y"
                      datePickerType="single"
                      light={true}
                    >
                      <DatePickerInput
                        id="date-picker-default-id"
                        placeholder="dd/mm/yyyy"
                        labelText={intl.formatMessage({
                          id: "patient.dob",
                          defaultMessage: "Date of Birth",
                        })}
                        type="text"
                        name={field.name}
                      />
                    </DatePicker>
                  )}
                </Field>
              </Column>
              <Column lg={8}>
                <Field name="gender">
                  {({ field }) => (
                    <RadioButtonGroup
                      defaultSelected=""
                      legendText={intl.formatMessage({
                        id: "patient.gender",
                        defaultMessage: "Gender",
                      })}
                      name={field.name}
                      id="search_patient_gender"
                    >
                      <RadioButton
                        id="search-radio-1"
                        labelText={intl.formatMessage({
                          id: "patient.male",
                          defaultMessage: "Male",
                        })}
                        value="M"
                      />
                      <RadioButton
                        id="search-radio-2"
                        labelText={intl.formatMessage({
                          id: "patient.female",
                          defaultMessage: "Female",
                        })}
                        value="F"
                      />
                    </RadioButtonGroup>
                  )}
                </Field>
              </Column>
              <Column lg={16}>
                {" "}
                <br />{" "}
              </Column>
              <Column lg={2}>
                <Button kind="tertiary">
                  <FormattedMessage
                    id="label.button.externalsearch"
                    defaultMessage="External Search"
                  />
                </Button>
              </Column>
              <Column lg={8}>
                <Button type="submit">
                  <FormattedMessage id="label.button.search" />
                </Button>
              </Column>
            </Grid>
          </Form>
        )}
      </Formik>
      <Column lg={16}>
        {" "}
        <br />{" "}
      </Column>
      <Column lg={16}>
        {pagination && (
          <Grid>
            <Column lg={11} />
            <Column lg={2}>
              <Button
                type=""
                id="loadpreviousresults"
                onClick={loadPreviousResultsPage}
                disabled={previousPage != null ? false : true}
              >
                <FormattedMessage id="button.label.loadprevious" />
              </Button>
            </Column>
            <Column lg={2}>
              <Button
                type=""
                id="loadnextresults"
                disabled={nextPage != null ? false : true}
                onClick={loadNextResultsPage}
              >
                <FormattedMessage id="button.label.loadnext" />
              </Button>
            </Column>
          </Grid>
        )}
      </Column>

      <Column lg={16}>
        <DataTable
          rows={patientSearchResults}
          headers={patientSearchHeaderData}
          isSortable
        >
          {({ rows, headers, getHeaderProps, getTableProps }) => (
            <TableContainer title="Patient Results">
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    <TableHeader></TableHeader>
                    {headers.map((header) => (
                      <TableHeader
                        key={header.key}
                        {...getHeaderProps({ header })}
                      >
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  <>
                    {rows
                      .slice((page - 1) * pageSize)
                      .slice(0, pageSize)
                      .map((row) => (
                        <TableRow key={row.id}>
                          <TableCell>
                            {" "}
                            <RadioButton
                              name="radio-group"
                              onClick={patientSelected}
                              labelText=""
                              id={row.id}
                            />
                          </TableCell>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          ))}
                        </TableRow>
                      ))}
                  </>
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>
        <Pagination
          onChange={handlePageChange}
          page={page}
          pageSize={pageSize}
          pageSizes={[5, 10, 20, 30]}
          totalItems={patientSearchResults.length}
          forwardText={intl.formatMessage({ id: "pagination.forward" })}
          backwardText={intl.formatMessage({ id: "pagination.backward" })}
          itemRangeText={(min, max, total) =>
            intl.formatMessage(
              { id: "pagination.item-range" },
              { min: min, max: max, total: total },
            )
          }
          itemsPerPageText={intl.formatMessage({
            id: "pagination.items-per-page",
          })}
          itemText={(min, max) =>
            intl.formatMessage(
              { id: "pagination.item" },
              { min: min, max: max },
            )
          }
          pageNumberText={intl.formatMessage({
            id: "pagination.page-number",
          })}
          pageRangeText={(_current, total) =>
            intl.formatMessage(
              { id: "pagination.page-range" },
              { total: total },
            )
          }
          pageText={(page, pagesUnknown) =>
            intl.formatMessage(
              { id: "pagination.page" },
              { page: pagesUnknown ? "" : page },
            )
          }
        />
      </Column>
    </>
  );
}

export default injectIntl(SearchPatientForm);
