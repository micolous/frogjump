/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 * This code was generated by https://github.com/google/apis-client-generator/
 * (build: 2015-11-16 19:10:01 UTC)
 * on 2015-12-15 at 13:23:05 UTC 
 * Modify at your own risk.
 */

package com.appspot.frogjump_cloud.frogjump.model;

/**
 * Model definition for FrogjumpApiMessagesJoinGroupRequest.
 *
 * <p> This is the Java data model class that specifies how to parse/serialize into the JSON that is
 * transmitted over HTTP when working with the frogjump. For a detailed explanation see:
 * <a href="https://developers.google.com/api-client-library/java/google-http-java-client/json">https://developers.google.com/api-client-library/java/google-http-java-client/json</a>
 * </p>
 *
 * @author Google, Inc.
 */
@SuppressWarnings("javadoc")
public final class FrogjumpApiMessagesJoinGroupRequest extends com.google.api.client.json.GenericJson {

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key("gcm_token")
  private java.lang.String gcmToken;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key("group_id")
  private java.lang.String groupId;

  /**
   * @return value or {@code null} for none
   */
  public java.lang.String getGcmToken() {
    return gcmToken;
  }

  /**
   * @param gcmToken gcmToken or {@code null} for none
   */
  public FrogjumpApiMessagesJoinGroupRequest setGcmToken(java.lang.String gcmToken) {
    this.gcmToken = gcmToken;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.String getGroupId() {
    return groupId;
  }

  /**
   * @param groupId groupId or {@code null} for none
   */
  public FrogjumpApiMessagesJoinGroupRequest setGroupId(java.lang.String groupId) {
    this.groupId = groupId;
    return this;
  }

  @Override
  public FrogjumpApiMessagesJoinGroupRequest set(String fieldName, Object value) {
    return (FrogjumpApiMessagesJoinGroupRequest) super.set(fieldName, value);
  }

  @Override
  public FrogjumpApiMessagesJoinGroupRequest clone() {
    return (FrogjumpApiMessagesJoinGroupRequest) super.clone();
  }

}
