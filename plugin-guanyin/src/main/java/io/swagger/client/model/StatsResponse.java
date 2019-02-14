/*
 * Cromwell Server REST API
 * Describes the REST API provided by a Cromwell server
 *
 * OpenAPI spec version: 30
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package io.swagger.client.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Objects;

/** Provides engine level statistics for things running inside the system */
@ApiModel(description = "Provides engine level statistics for things running inside the system")
@javax.annotation.Generated(
    value = "io.swagger.codegen.languages.JavaClientCodegen",
    date = "2018-12-03T20:33:09.260Z")
public class StatsResponse {
  @SerializedName("workflows")
  private Integer workflows = null;

  @SerializedName("jobs")
  private Integer jobs = null;

  public StatsResponse workflows(Integer workflows) {
    this.workflows = workflows;
    return this;
  }

  /**
   * The number of currently running workflows
   *
   * @return workflows
   */
  @ApiModelProperty(required = true, value = "The number of currently running workflows")
  public Integer getWorkflows() {
    return workflows;
  }

  public void setWorkflows(Integer workflows) {
    this.workflows = workflows;
  }

  public StatsResponse jobs(Integer jobs) {
    this.jobs = jobs;
    return this;
  }

  /**
   * The number of currently running jobs
   *
   * @return jobs
   */
  @ApiModelProperty(required = true, value = "The number of currently running jobs")
  public Integer getJobs() {
    return jobs;
  }

  public void setJobs(Integer jobs) {
    this.jobs = jobs;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StatsResponse statsResponse = (StatsResponse) o;
    return Objects.equals(this.workflows, statsResponse.workflows)
        && Objects.equals(this.jobs, statsResponse.jobs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workflows, jobs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StatsResponse {\n");

    sb.append("    workflows: ").append(toIndentedString(workflows)).append("\n");
    sb.append("    jobs: ").append(toIndentedString(jobs)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}