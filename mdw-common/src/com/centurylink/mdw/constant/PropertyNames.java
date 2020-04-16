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
    public static final String MDW_LOGGING_ACTIVITY_ENABLED = "mdw.logging.activity.enabled";
    public static final String MDW_LOGGING_ACTIVITY_CLEANUP_RETAIN = "mdw.logging.activity.cleanup.retain";
    // only set in-memory by test runner "<host>:<port>:<timeout>";
    public static final String MDW_LOGGING_WATCHER = "mdw.logging.watcher";
    // log roller
    public static final String MDW_LOG_ROLLER_ENABLED = "mdw.logging.roller.enabled";
    public static final String MDW_LOG_ROLLER_RETAIN = "mdw.logging.roller.retain"; // days
    public static final String MDW_LOG_ROLLER_FILES = "mdw.logging.roller.files";

    // container service providers
    public static final String MDW_CONTAINER_DATASOURCE_PROVIDER = "mdw.container.datasource.provider";
    public static final String MDW_CONTAINER_JMS_PROVIDER = "mdw.container.jms.provider";
    public static final String MDW_CONTAINER_MESSENGER = "mdw.container.messenger";  // jms or http
    public static final String MDW_CONTAINER_THREADPOOL_PROVIDER = "mdw.container.threadpool.provider";
    public static final String MDW_CONTAINER_CLASSIC_CLASSLOADING = "mdw.container.classic.classloading";
    // database
    public static final String MDW_DB_URL = "mdw.database.url";
    public static final String MDW_DB_USERNAME = "mdw.database.username";
    @SuppressWarnings("squid:S2068")
    public static final String MDW_DB_PASSWORD = "mdw.database.password";
    public static final String MDW_DB_VERSION_SUPPORTED = "mdw.database.version.supported";     // lowest version supported
    // If true, then will save datetime/timestamp using microsecond precision - Depends on declaring table columns correctly
    public static final String MDW_DB_MICROSECOND_PRECISION = "mdw.database.microsecond.precision";
    // for embedded db
    public static final String MDW_EMBEDDED_DB_BASE_LOC = "mdw.db.base.location";
    public static final String MDW_EMBEDDED_DB_DATA_LOC = "mdw.db.data.location";
    public static final String MDW_EMBEDDED_DB_STARTUP = "mdw.db.startup";

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
    public static final String MDW_SERVERS = "servers"; // server configuration in yaml form
    public static final String MDW_SERVER_LIST = "mdw.server.list"; // host1:port1,host2:port2,...  Represents MDW worker instances
    public static final String MDW_REMOTE_SERVER = "mdw.remote.server";
    public static final String MDW_ACTIVITY_ACTIVE_MAX_RETRY = "mdw.activity.active.max.retry";
    public static final String MDW_PERFORMANCE_LEVEL_SERVICE = "mdw.performance.level.service";
    public static final String MDW_PERFORMANCE_LEVEL_REGULAR = "mdw.performance.level.regular";
    public static final String MDW_ENGINE_USE_TRANSACTION = "mdw.engine.use.transaction";
    public static final String MDW_STUB_SERVER = "mdw.stub.server"; // used internally, set by regression tester "<host>:<port>:<timeout>";
    public static final String DOCS_URL = "mdw.docs.url";
    public static final String DISCOVERY_URL = "mdw.discovery.urls";
    public static final String FILEPANEL_ROOT_DIRS = "filepanel.root.dirs";
    public static final String FILEPANEL_EXCLUDE_PATTERNS = "filepanel.exclude.patterns";
    public static final String FILEPANEL_MASKED_LINES = "filepanel.masked.lines";
    public static final String MAX_DOWNLOAD_BYTES = "mdw.max.download.bytes";
    public static final String MDW_PROCESS_LAUNCH_DELAY = "mdw.process.launch.delay";
    public static final String MDW_INTERNAL_EVENT_CONSUME_RETRY_SLEEP = "mdw.internal.event.consume.retry.sleep";
    public static final String MDW_INTERNAL_EVENT_DEV_CLEANUP = "mdw.internal.event.dev.cleanup";
    public static final String MDW_TEMP_DIR = "mdw.temp.dir";
    public static final String MDW_ATTACHMENTS_DIR = "mdw.attachments.dir";

    public static final String MDW_APP_ID = "mdw.app.id";
    public static final String MDW_WAR_NAME = "mdw.war.name";
    public static final String MDW_SERVICES_URL = "mdw.services.url";
    public static final String ACTIVITY_RESUME_DELAY = "mdw.activity.resume.delay";
    public static final String TASK_NOTICE_EMAIL_FROM = "mdw.task.notice.email.from";
    public static final String TASK_RESUME_NOTIFY_ENDPOINT = "mdw.task.resume.notify.endpoint";
    public static final String MDW_WEBSOCKET_URL = "mdw.websocket.url";

    public static final String MDW_JAVA_COMPILER_OPTIONS = "mdw.java.compiler.options";
    public static final String MDW_JAVA_LIBRARY_PATH = "mdw.java.library.path";
    public static final String MDW_JAVA_COMPILER_CLASSPATH = "mdw.java.compiler.classpath";
    public static final String MDW_JAVA_RUNTIME_CLASSPATH = "mdw.java.runtime.classpath";

    // this is a group, contains <name>.ClassName/Directory/FilenamePattern/IntervalMinutes/DelayMinutes
    public static final String MDW_LISTENER_FILE = "mdw.listener.file"; // prefix for file listener properties

    // this is a group, contains <name>.TimerClass/Schedule
    public static final String MDW_TIMER_TASK = "mdw.timer.task";   // prefix for timer task properties

    // TODO: old-style format
    public static final String PROCESS_CLEANUP = "MDWFramework.ProcessCleanup";

    public static final String UNSCHEDULED_EVENTS_CHECK_DELAY = "mdw.unscheduled.events.check.delay"; // seconds
    public static final String UNSCHEDULED_EVENTS_CHECK_INTERVAL = "mdw.unscheduled.events.check.interval"; // seconds
    public static final String UNSCHEDULED_EVENTS_BATCH_SIZE = "mdw.unscheduled.events.max.batch.size";
    public static final String UNSCHEDULED_EVENTS_MIN_AGE = "mdw.unscheduled.events.min.age";  // seconds

    public static final String SCHEDULED_EVENTS_BATCH_SIZE = "mdw.scheduled.events.max.batch.size";
    public static final String SCHEDULED_EVENTS_MEMORY_RANGE = "mdw.scheduled.events.memory.range";  // minutes

    public static final String MDW_TIMER_INITIAL_DELAY = "mdw.timer.InitialDelay";  // delay of first check in seconds
    public static final String MDW_TIMER_CHECK_INTERVAL = "mdw.timer.CheckInterval";    // interval between checks in seconds
    public static final String MDW_TIMER_THRESHOLD_FOR_DELAY = "mdw.timer.ThresholdForDelay";   // (minutes) threshold for using timer for delayed JMS messages

    public static final String MDW_HUB_URL = "mdw.hub.url";
    public static final String MDW_TASK_ACTION_DEF = "mdw.hub.action.definition";
    public static final String MDW_HUB_OVERRIDE_PACKAGE = "mdw.hub.override.package";
    // root package maps to / context path for JSX assets
    public static final String MDW_HUB_ROOT_PACKAGE = "mdw.hub.root.package";

    // XmlOptions properties
    public static final String MDW_TRANSLATOR_XMLBEANS_LOAD_OPTIONS = "mdw.translator.xmlbeans.load.options";
    public static final String MDW_TRANSLATOR_XMLBEANS_SAVE_OPTIONS = "mdw.translator.xmlbeans.save.options";

    // vcs-based asset persistence
    public static final String MDW_ASSET_LOCATION = "mdw.asset.location";
    public static final String MDW_ASSET_SYNC_ENABLED = "mdw.asset.sync.enabled";
    public static final String MDW_ASSET_SYNC_INTERVAL = "mdw.asset.sync.interval";
    public static final String MDW_ASSET_SYNC_GITRESET = "mdw.asset.sync.gitreset";
    public static final String MDW_ASSET_REF_AUTOPOP = "mdw.asset.ref.autopop";
    public static final String MDW_GIT_LOCAL_PATH = "mdw.git.local.path";
    public static final String MDW_GIT_REMOTE_URL = "mdw.git.remote.url";
    public static final String MDW_GIT_USER = "mdw.git.user";
    @SuppressWarnings("squid:S2068")
    public static final String MDW_GIT_PASSWORD = "mdw.git.password";
    public static final String MDW_GIT_BRANCH = "mdw.git.branch";
    public static final String MDW_GIT_TAG = "mdw.git.tag";
    public static final String MDW_GIT_TRUSTED_HOST = "mdw.git.trusted.host";
    public static final String MDW_GIT_FETCH = "mdw.git.fetch";
    public static final String MDW_GIT_AUTO_PULL = "mdw.git.auto.pull";
    public static final String MDW_GIT_AUTO_CHECKOUT = "mdw.git.auto.checkout";
    public static final String MDW_GITHUB_SECRET_TOKEN = "MDW_GITHUB_SECRET_TOKEN";

    // automated testing
    public static final String MDW_TEST_RESULTS_LOCATION = "mdw.test.results.location";
    public static final String MDW_TEST_SUMMARY_FILE = "mdw.test.summary.file";

    // Ldap configuration
    public static final String MDW_LDAP_PROTOCOL = "mdw.ldap.protocol";
    public static final String MDW_LDAP_HOST = "mdw.ldap.host";
    public static final String MDW_LDAP_PORT = "mdw.ldap.port";
    public static final String MDW_LDAP_BASE_DN = "mdw.ldap.base.dn";

    // Authentication / MDW Central
    public static final String MDW_AUTH_TOKEN_MAX_AGE = "mdw.auth.token.maxage";  // Max age in seconds that a JWT token will still be considered valid
    public static final String MDW_CENTRAL_URL = "mdw.central.url";
    public static final String MDW_CENTRAL_AUTH_URL = "mdw.central.auth.url";
    public static final String MDW_CENTRAL_ROUTING_URL = "mdw.central.routing.url";

    // Authentication / Custom JWT providers
    public static final String MDW_JWT = "mdw.jwt"; // Group name (i.e mdw.jwt.myissuer1.(issuer|key|userClaim,etc))
    public static final String MDW_JWT_ISSUER = "issuer";  // Required if using a JWT provider other than MDW
    public static final String MDW_JWT_KEY = "key";  // Required if using a JWT provider other than MDW
    public static final String MDW_JWT_USER_CLAIM = "userClaim";  // Required if using a JWT provider other than MDW - name of Claim containing authenticated user
    public static final String MDW_JWT_SUBJECT = "subject"; // Optional JWT field to verify
    public static final String MDW_JWT_ALGORITHM = "algorithm";  // Optional for additional safety check

    public static final String MDW_JWT_PRESERVE = "mdw.jwt.preserve";  // Set to true for setting header with Authenticated/verified JWT, for use by services

    // Request Routing
    public static final String MDW_ROUTING_REQUESTS_ENABLED = "mdw.requestrouting.enabled";
    public static final String MDW_ROUTING_REQUESTS_HTTPS_ENABLED = "mdw.requestrouting.https.enabled";
    public static final String MDW_ROUTING_REQUESTS_DEFAULT_STRATEGY = "mdw.requestrouting.default.strategy";
    public static final String MDW_ROUTING_SERVERS = "routing.servers";
    public static final String MDW_ROUTING_SERVER_LIST = "mdw.requestrouting.server.list";    // host1:port1,host2:port2,... Represents MDW routing instances
    public static final String MDW_ROUTING_ACTIVE_SERVER_INTERVAL = "mdw.requestrouting.active.server.interval";  // Interval in seconds for checking server status
    public static final String MDW_ROUTING_REQUEST_TIMEOUT = "mdw.requestrouting.timeout";  // Seconds to wait for response from routed to server

    // Mail
    public static final String MDW_MAIL_SMTP_HOST = "mdw.mail.smtp.host";
    public static final String MDW_MAIL_SMTP_TIMEOUT = "mdw.mail.smtp.timeout";
    public static final String MDW_MAIL_SMTP_PORT = "mdw.mail.smtp.port";
    public static final String MDW_MAIL_SMTP_USER = "mdw.mail.smtp.user";
    public static final String MDW_MAIL_SMTP_PASS = "mdw.mail.smtp.pass";
    public static final String MDW_MAIL_CONNECTION_TIMEOUT = "mdw.mail.connection.timeout";

    // Transaction Retry
    public static final String MDW_TRANSACTION_RETRY_INTERVAL = "mdw.transaction.retry.interval";
    public static final String MDW_TRANSACTION_RETRY_MAX = "mdw.transaction.retry.max";

    public static final String MDW_LISTENER_KAFKA = "mdw.listener.kafka";

    public static final String MDW_USERGROUP_MONITOR_INTERVAL = "mdw.usergroupmonitor.interval";

    public static final String MDW_JSON_FALSE_VALUES_OUTPUT = "mdw.json.false.values.output";
    public static final String MDW_JSON_PRETTY_INDENT = "mdw.json.pretty.indent";
    public static final String MDW_JSON_ORDERED_KEYS = "mdw.json.ordered.keys";
    public static final String MDW_DISCOVERY_BRANCHTAGS_MAX = "mdw.discovery.maxBranchesTags";

    // system metrics
    public static final String MDW_SYSTEM_METRICS_PERIOD = "mdw.system.metrics.period";
    public static final String MDW_SYSTEM_METRICS_RETENTION = "mdw.system.metrics.retention";
    public static final String MDW_SYSTEM_METRICS_ENABLED = "mdw.system.metrics.enabled";
    public static final String MDW_SYSTEM_METRICS_LOCATION = "mdw.system.metrics.location";
    public static final String MDW_SYSTEM_METRICS_BYTES = "mdw.system.metrics.bytes";

    // milestones
    public static final String MDW_MILESTONE_GROUPS = "mdw.milestone.groups";
    public static final String MDW_MILESTONE_IGNORES = "mdw.milestone.ignores";
    public static final String MDW_MILESTONE_MAX_DEPTH = "mdw.milestone.maxDepth";

    // mongo
    public static final String MDW_MONGODB_FORMAT_JSON= "mdw.mongodb.format.json";

    // wait activity fallback
    public static final String MDW_WAIT_FALLBACK_STAGGER = "mdw.wait.fallback.stagger";
    public static final String MDW_WAIT_FALLBACK_MAX = "mdw.wait.fallback.max";
    public static final String MDW_WAIT_FALLBACK_AGE = "mdw.wait.fallback.age";
    public static final String MDW_WAIT_FALLBACK_ARCHIVED = "mdw.wait.fallback.archived";

    //issue 828
    public static final String MDW_ADAPTER_READ_TIMEOUT= "mdw.adapter.read.timeout";
    public static final String MDW_ADAPTER_CONNECTION_TIMEOUT = "mdw.adapter.connect.timeout";


}