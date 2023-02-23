CREATE TABLE infra_request_log
(
    id             uuid primary key,
    project        varchar(20)  NOT NULL,
    function       varchar(30)  NOT NULL,
    source         varchar(30)  NOT NULL,
    tenant_id      varchar(20),
    principal_id   varchar(64),
    tags           text,
    user_type      varchar(20),
    user_id        uuid,
    run_as         bool,
    trace_id       char(32),
    req_id         varchar(36)  NOT NULL,
    req_at         timestamp    NOT NULL,
    api            varchar(255) NOT NULL,
    headers        text,
    querystring    text,
    req_body       text,
    ip             varchar(39),
    client_id      varchar(36),
    client_version varchar(10),
    rsp_at         timestamp    NOT NULL,
    rsp_status     integer      NOT NULL,
    rsp_body       text,
    duration       integer      NOT NULL
);

create index infra_request_log_req_at_idx on infra_request_log (req_at);
create index infra_request_log_trace_id_idx on infra_request_log (trace_id);
create index infra_request_log_req_id_idx on infra_request_log (req_id);
create index infra_request_log_project_api_idx on infra_request_log (project, api);
create index infra_request_log_duration_idx on infra_request_log (duration);

CREATE TABLE infra_error_log
(
    id               uuid primary key,
    occur_at         timestamp   NOT NULL,
    error_code       char(4)     NOT NULL,
    error_code_type  varchar(20) NOT NULL,
    error_msg        text,
    stacktrace       text,
    extras           text,
    project          varchar(20) NOT NULL,
    function         varchar(30) NOT NULL,
    source           varchar(30) NOT NULL,
    tenant_id        varchar(20),
    principal_id     varchar(64),
    tags             text,
    user_type        varchar(20),
    user_id          uuid,
    run_as           bool,
    trace_id         char(32),
    req_id           varchar(36),
    req_at           timestamp,
    api              varchar(255),
    headers          text,
    querystring      text,
    req_body         text,
    ip               varchar(39),
    client_id        varchar(36),
    client_version   varchar(10),
    rsp_at           timestamp,
    rsp_status       integer,
    rsp_body         text,
    duration         integer,
    service_name     varchar(30),
    service_api      varchar(255),
    service_req_id   varchar(255),
    service_req_at   timestamp,
    service_req_body text,
    service_rsp_code varchar(20),
    service_rsp_at   timestamp,
    service_rsp_body text,
    service_duration integer
);

create index infra_error_log_occur_at_idx on infra_error_log (occur_at);
create index infra_error_log_trace_id_idx on infra_error_log (trace_id);
create index infra_error_log_req_id_idx on infra_error_log (req_id);
create index infra_error_log_project_api_idx on infra_error_log (project, api);
create index infra_error_log_service_name_req_id_idx on infra_error_log (service_name, service_req_id);

CREATE TABLE infra_login_log
(
    id             uuid primary key,
    trace_id       char(32),
    user_id        uuid        NOT NULL,
    result_code    varchar(20) NOT NULL,
    occur_at       timestamp   NOT NULL,
    project        varchar(20) NOT NULL,
    source         varchar(30) NOT NULL,
    tenant_id      varchar(20),
    client_id      varchar(36),
    client_version varchar(10),
    ip             varchar(39),
    sid            varchar(120)
);

create index infra_login_log_trace_id_idx on infra_login_log (trace_id);
create index infra_login_log_user_id_idx on infra_login_log (user_id);
create index infra_login_log_result_code_idx on infra_login_log (result_code);
create index infra_login_log_occur_at_idx on infra_login_log (occur_at);
create index infra_login_log_tenant_id_idx on infra_login_log (tenant_id);

CREATE TABLE infra_notification_message_log
(
    id                    uuid primary key,
    notification_id       uuid        NOT NULL,
    trace_id              char(32),
    event_id              uuid        NOT NULL,
    type                  varchar(30) NOT NULL,
    version               varchar(5),
    channel               varchar(20) NOT NULL,
    lang                  varchar(20) NOT NULL,
    send_at               timestamp,
    error_msg             text,
    receivers             text        NOT NULL,
    content               text,
    success               bool        NOT NULL,
    success_list          text,
    failure_list          text,
    invalid_recipient_ids text,
    rsp_code              varchar(30),
    rsp_msg               text,
    rsp_at                timestamp,
    rsp_body              text,
    duration              integer
);

create index infra_notification_log_notification_id_idx on infra_notification_message_log (notification_id);
create index infra_notification_log_trace_id_idx on infra_notification_message_log (trace_id);
create index infra_notification_log_event_id_idx on infra_notification_message_log (event_id);
create index infra_notification_log_type_channel_idx on infra_notification_message_log (type, channel);
create index infra_notification_log_send_at_idx on infra_notification_message_log (send_at);
