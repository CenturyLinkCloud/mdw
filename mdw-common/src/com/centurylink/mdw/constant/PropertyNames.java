/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.constant;

public class PropertyNames {

    // logging
    public static final String MDW_LOGGING_LEVEL = "mdw.logging.level";
    public static final String MDW_LOGGING_FILE = "mdw.logging.file";
    public static final String MDW_LOGGING_DIR = "mdw.logging.dir";
    public static final String MDW_LOGGING_WATCHER = "mdw.logging.watcher"; // used internally, set by regression tester "<host>:<port>:<timeout>";
    public static final String MDW_LOGGING_WEB_WATCHER = "mdw.logging.web.watcher"; // used for watching using WebSocketServer
    // container service providers
    public static final String MDW_CONTAINER_DATASOURCE_PROVIDER= "mdw.container.datasource_provider";
    public static final String MDW_CONTAINER_JMS_PROVIDER= "mdw.container.jms_provider";
    public static final String MDW_CONTAINER_MESSENGER= "mdw.container.messenger";  // jms or rmi
    public static final String MDW_CONTAINER_THREADPOOL_PROVIDER= "mdw.container.threadpool_provider";
    // database
    public static final String MDW_DB_URL = "mdw.database.url";
    public static final String MDW_DB_USERNAME = "mdw.database.username";
    public static final String MDW_DB_PASSWORD = "mdw.database.password";
    public static final String MDW_DB_VERSION = "mdw.database.version";
    public static final String MDW_DB_VERSION_SUPPORTED = "mdw.database.version.supported";     // lowest version supported
    public static final String MDW_DB_POOLSIZE = "mdw.database.poolsize";
    public static final String MDW_DB_BORROW_TIMEOUT = "mdw.database.borrow.timeout";   // seconds; 0 - block indefinitely, -1 - fail right away
    public static final String MDW_DB_TRACE = "mdw.database.trace";     // none, query, timing
    // for embedded db
    public static final String MDW_DB_BASE_LOC = "mdw.db.base.location";
    public static final String MDW_DB_DATA_LOC = "mdw.db.data.location";
    public static final String MDW_DB_EMBEDDED_HOST_PORT = "mdw.db.embedded.server";
    // for MongoDB
    public static final String MDW_MONGODB_HOST = "mdw.mongodb.host";
    public static final String MDW_MONGODB_PORT = "mdw.mongodb.port";
    public static final String MDW_MONGODB_POOLSIZE = "mdw.mongodb.poolsize";
    // file system
    public static final String MDW_FS_USER = "mdw.file.system.user";
    public static final String MDW_FS_PASSWORD = "mdw.file.system.password";
    public static final String MDW_MAX_UPLOAD_BYTES = "mdw.max.upload.bytes";

    // thread pool
    public static final String MDW_THREADPOOL_CORE_THREADS = "mdw.threadpool.core_threads";
    public static final String MDW_THREADPOOL_MAX_THREADS = "mdw.threadpool.max_threads";
    public static final String MDW_THREADPOOL_QUEUE_SIZE = "mdw.threadpool.queue_size";
    public static final String MDW_THREADPOOL_KEEP_ALIVE = "mdw.threadpool.keep_alive";
    public static final String MDW_THREADPOOL_TERMINATION_TIMEOUT = "mdw.threadpool.termination_timeout";
    public static final String MDW_THREADPOOL_WORKER = "mdw.threadpool.worker"; // prefix, add <worker_name>.min_threads/max_threads
    // jms listener
    public static final String MDW_JMS_LISTENER_POLL_INTERVAL = "mdw.jms.listener.poll.interval";
    public static final String MDW_JMS_LISTENER_RECEIVE_TIMEOUT = "mdw.jms.listener.receive.timeout";
    public static final int MDW_JMS_LISTENER_POLL_INTERVAL_DEFAULT = 5;

    // misc
    public static final String MDW_SERVER_LIST = "mdw.server.list"; // host1:port1,host2:port2,...  Represents MDW worker instances
    public static final String MDW_FILE_DIR = "mdw.file.dir";
    public static final String MDW_REMOTE_SERVER = "mdw.remote.server";
    public static final String MDW_ACTIVITY_ACTIVE_MAX_RETRY = "mdw.activity.active.max.retry";
    public static final String MDW_WEB_SESSION_TIMEOUT = "mdw.web.session.timeout";
    public static final String MDW_PERFORMANCE_LEVEL_SERVICE = "mdw.performance.level.service";
    public static final String MDW_PERFORMANCE_LEVEL_REGULAR = "mdw.performance.level.regular";
    public static final String MDW_ENGINE_USE_TRANSACTION = "mdw.engine.use.transaction";
    public static final String MDW_STUB_SERVER = "mdw.stub.server"; // used internally, set by regression tester "<host>:<port>:<timeout>";
    public static final String WEBTOOLS_URL = "mdw.webtools.url";
    public static final String DOCS_URL = "mdw.docs.url";
    public static final String DISCOVERY_URL = "mdw.discovery.url";
    public static final String FILEPANEL_ROOT_DIRS = "mdw.filepanel.root.dirs";
    public static final String FILEPANEL_CONFIG_DIRS = "mdw.filepanel.config.dirs";
    public static final String FILEPANEL_EXCLUDE_PATTERNS = "mdw.filepanel.exclude.patterns";
    public static final String FILEPANEL_BINARY_PATTERNS = "mdw.filepanel.binary.patterns";
    public static final String FILEPANEL_MASKED_LINES = "mdw.filepanel.masked.lines";
    public static final String MDW_PROCESS_LAUNCH_DELAY = "mdw.process.launch.delay";
    public static final String MDW_INTERNAL_EVENT_CONSUME_RETRY_SLEEP = "mdw.internal.event.consume.retry.sleep";
    public static final String MDW_INTERNAL_EVENT_DEV_CLEANUP = "mdw.internal.event.dev.cleanup";
    public static final String MDW_TEMP_DIR = "mdw.temp.dir";

    public static final String APPLICATION_NAME = "mdw.application.name";
    public static final String MDW_WAR_NAME = "mdw.war.name";
    public static final String TASK_MANAGER_URL_OLD = "MDWFramework.TaskManagerWeb/task.manager.url";
    public static final String TASK_MANAGER_URL = "mdw.task.manager.url";
    public static final String TASK_MANAGER_WELCOME_PATH = "MDWFramework.TaskManagerWeb/task.manager.welcome.path";
    public static final String MDW_WEB_URL_OLD = "MDWFramework.MDWDesigner/helpers.url";
    public static final String MDW_WEB_URL = "mdw.web.url";
    public static final String MDW_SERVICES_URL_OLD = "MDWFramework.MDWDesigner/services.url";
    public static final String MDW_SERVICES_URL = "mdw.services.url";
    public static final String ACTIVITY_RESUME_DELAY = "MDWFramework.WorkflowEngine/ActivityResumeDelay";
    public static final String MDW_CONFIG_DIRECOTRY = "MDWFramework.ApplicationDetails/ConfigDir";
    public static final String TASK_MANAGER_UI_DEF_FILE = "MDWFramework.TaskManagerWeb/view.ui.definition.file";
    public static final String TASK_MANAGER_ACTIONS_FILE = "MDWFramework.TaskManagerWeb/ui.task.actions.file";
    public static final String ATTACHMENTS_STORAGE_LOCATION = "MDWFramework.TaskManagerWeb/attachments.storage.location";
    public static final String ATTACHMENTS_DOWNLOAD_SERVLET_URL = "MDWFramework.TaskManagerWeb/attachments.download.servlet.url";
    public static final String WEB_RENDER_ERROR_DETAILS = "MDWFramework.TaskManagerWeb/render.error.details";
    public static final String NOTIFICATION_EMAIL_FROM_ADDRESS = "MDWFramework.TaskManagerWeb/task.notice.email.from.address";
    public static final String MDW_TASKMGR_PROCESS_DYNAMIC_JAVA_JSF_ANNOTATIONS = "mdw.taskmgr.dynamic.java.jsf.annotations";
    public static final String MDW_DYNAMIC_JAVA_COMPILE_OPTIONS = "mdw.dynamic.java.compile.options";
    public static final String TASK_RESUME_NOTIFY_ENDPOINT = "mdw.task.resume.notify.endpoint";
    public static final String MDW_WEBSOCKET_URL = "mdw.websocket.url";

    public static final String MDW_JAR_LIBRARY_PATH = "mdw.jar.library.path";
    public static final String MDW_COMPILER_CLASSPATH = "mdw.compiler.classpath";
    public static final String MDW_CLASSPATH = "mdw.classpath";

    public static final String MDW_BUS_ACCOUNT = "mdw.bus.account";
    public static final String MDW_BUS_URI = "mdw.bus.uri";

    // this is a group, contains <name>.topic/uri/queueSize/minWorker/maxWorker/dqName
    public static final String MDW_LISTENER_BUS = "mdw.listener.bus";   // prefix for bus listener properties

    // this is a group, contains <name>.ClassName/Directory/FilenamePattern/IntervalMinutes/DelayMinutes
    public static final String MDW_LISTENER_FILE = "mdw.listener.file"; // prefix for file listener properties

    // this is a group, for multiple MQ listeners
    public static final String MDW_LISTENER_MQ = "mdw.listener.mq";

    // this is a group, contains <name>.TimerClass/Schedule
    public static final String MDW_TIMER_TASK = "mdw.timer.task";   // prefix for timer task properties

    public static final String UNSCHEDULED_EVENTS_CHECK_DELAY = "mdw.unscheduled.events.check.delay"; // seconds
    public static final String UNSCHEDULED_EVENTS_CHECK_INTERVAL = "mdw.unscheduled.events.check.interval"; // seconds
    public static final String UNSCHEDULED_EVENTS_BATCH_SIZE = "mdw.unscheduled.events.max.batch.size";
    public static final String UNSCHEDULED_EVENTS_MIN_AGE = "mdw.unscheduled.events.min.age";  // seconds

    public static final String SCHEDULED_EVENTS_BATCH_SIZE = "mdw.scheduled.events.max.batch.size";
    public static final String SCHEDULED_EVENTS_MEMORY_RANGE = "mdw.scheduled.events.memory.range";  // seconds

    public static final String MDW_TIMER_INITIAL_DELAY = "mdw.timer.InitialDelay";  // delay of first check in seconds
    public static final String MDW_TIMER_CHECK_INTERVAL = "mdw.timer.CheckInterval";    // interval between checks in seconds
    public static final String MDW_TIMER_THRESHOLD_FOR_DELAY = "mdw.timer.ThresholdForDelay";   // (minutes) threshold for using timer for delayed JMS messages

    public static final String MDW_CERTIFIED_MESSAGE_INITIAL_DELAY = "mdw.certified_message.InitialDelay";  // delay of first check in seconds
    public static final String MDW_CERTIFIED_MESSAGE_CHECK_INTERVAL = "mdw.certified_message.CheckInterval";    // interval between checks in seconds
    public static final String MDW_CERTIFIED_MESSAGE_ACK_TIMEOUT = "mdw.certified_message.AcknowlegmentTimeout";    // default ack timeout in seconds
    public static final String MDW_CERTIFIED_MESSAGE_RETRY_INTERVAL = "mdw.certified_message.RetryInterval";    // default interval for next retry in seconds
    public static final String MDW_CERTIFIED_MESSAGE_MAX_TRIES = "mdw.certified_message.MaxTries";  // default max tries

    // this is a group, contains <name>.Adapter and pool specific properties
    public static final String MDW_CONNECTION_POOL = "mdw.connection.pool"; // prefix for connection pool properties

    public static final String MDW_SERVER_PROXY = "mdw.server.proxy";

    public static final String MDW_HUB_URL = "mdw.hub.url";
    public static final String MDW_DEFAULT_RENDERING_ENGINE = "mdw.default.rendering.engine";
    public static final String MDW_HUB_VIEW_DEF = "mdw.hub.view.definition";
    public static final String MDW_HUB_ACTION_DEF = "mdw.hub.action.definition";
    public static final String MDW_BLV_TOPIC = "mdw.blv.topic.name";
    public static final String MDW_HUB_PROCESS_DYNAMIC_JAVA_JSF_ANNOTATIONS = "mdw.hub.dynamic.java.jsf.annotations";
    public static final String MDW_HUB_OVERRIDE_PACKAGE = "mdw.hub.override.package";
    public static final String MDW_TASKS_UI = "mdw.tasks.ui";

    public static final String MDW_ADMIN_URL = "mdw.admin.url";
    public static final String MDW_DASHBOARD_URL = "mdw.dashboard.url";
    public static final String MDW_SOLUTIONS_URL = "mdw.solutions.url";

    public static final String MDW_SCRIPT_EXECUTOR = "mdw.script.executor";

    // XmlOptions properties
    public static final String MDW_TRANSLATOR_XMLBEANS_LOAD_OPTIONS = "mdw.translator.xmlbeans.load.options";
    public static final String MDW_TRANSLATOR_XMLBEANS_SAVE_OPTIONS = "mdw.translator.xmlbeans.save.options";

    // vcs-based asset persistence
    public static final String MDW_ASSET_LOCATION = "mdw.asset.location";
    public static final String MDW_GIT_LOCAL_PATH = "mdw.git.local.path";
    public static final String MDW_GIT_REMOTE_URL = "mdw.git.remote.url";
    public static final String MDW_GIT_USER = "mdw.git.user";
    public static final String MDW_GIT_PASSWORD = "mdw.git.password";
    public static final String MDW_GIT_BRANCH = "mdw.git.branch";
    public static final String MDW_GIT_TRUSTED_HOST = "mdw.git.trusted.host";
    public static final String MDW_GIT_AUTO_PULL = "mdw.git.auto.pull";
    public static final String MDW_GITHUB_SECRET_TOKEN = "mdw.github.secret.token";
    public static final String MDW_TEAM_SLACK_CHANNEL = "mdw.slack.team";

    // Task with these status will not be cancelled when process instance completes
    public static final String FINAL_TASK_STATUSES = "mdw.task.statuses.final";

    // automated testing
    public static final String MDW_TEST_RESULTS_LOCATION = "mdw.test.results.location";
    public static final String MDW_FUNCTION_TESTS_SUMMARY_FILE = "mdw.function.tests.summary.file";
    public static final String MDW_FEATURE_TESTS_SUMMARY_FILE = "mdw.feature.tests.summary.file";

    // Ldap configuration
    public static final String LDAP_PROTOCOL = "LDAP/Protocol";
    public static final String LDAP_HOST = "LDAP/Host";
    public static final String LDAP_PORT = "LDAP/Port";
    public static final String BASE_DN = "LDAP/BaseDN";
    public static final String APP_CUID = "LDAP/AppCUID";
    public static final String APP_PASSWORD = "LDAP/AppPassword";

    // Authentication
    public static final String HTTP_BASIC_AUTH_MODE = "mdw.services.http.basic.auth";

    // Request Routing
    public static final String MDW_ROUTING_REQUESTS_ENABLED = "mdw.requestrouting.enabled";
    public static final String MDW_ROUTING_REQUESTS_HTTPS_ENABLED = "mdw.requestrouting.https.enabled";
    public static final String MDW_ROUTING_REQUESTS_DEFAULT_STRATEGY = "mdw.requestrouting.default.strategy";
    public static final String MDW_ROUTING_SERVER_LIST = "mdw.requestrouting.server.list";    // host1:port1,host2:port2,... Represents MDW routing instances
    public static final String MDW_ROUTING_ACTIVE_SERVER_INTERVAL = "mdw.requestrouting.active.server.interval";  // Interval in seconds for checking server status
    public static final String MDW_ROUTING_REQUEST_TIMEOUT = "mdw.requestrouting.timeout";  // Seconds to wait for response from routed to server

    // Mail
    public static final String MDW_MAIL_CONNECTION_TIMEOUT = "mdw.mail.connectiontimeout";
    public static final String MDW_MAIL_SMTP_HOST = "mdw.mail.smtp.host";
    public static final String MDW_MAIL_SMTP_TIMEOUT = "mdw.mail.smtp.timeout";
    public static final String MDW_MAIL_SMTP_PORT = "mdw.mail.smtp.port";
    public static final String MDW_MAIL_SMTP_USER = "mdw.mail.smtp.user";
    public static final String MDW_MAIL_SMTP_PASS = "mdw.mail.smtp.pass";

    // oauth
    public static final String MDW_OAUTH_REST_ENDPOINT = "mdw.oauth.rest.endpoint";
    public static final String MDW_OAUTH_REST_APP_DOMAIN = "mdw.oauth.rest.app.domain";
    public static final String MDW_OAUTH_REST_USER_DOMAIN = "mdw.oauth.rest.user.domain";
    public static final String MDW_OAUTH_REST_HEADERS = "mdw.oauth.rest.headers";
}