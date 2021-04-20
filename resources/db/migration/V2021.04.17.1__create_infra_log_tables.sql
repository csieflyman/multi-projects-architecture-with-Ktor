CREATE TABLE infra_request_log
(
    id uuid primary key,
    req_id varchar(36) NOT NULL,
    req_time timestamp NOT NULL,
    api varchar(255) NOT NULL,
    querystring text,
    req_body text,
    project varchar(20) NOT NULL,
    function varchar(30) NOT NULL,
    tag varchar(30),
    source varchar(30) NOT NULL,
    tenant_id varchar(20),
    principal varchar(36) NOT NULL,
    run_as bool NOT NULL,
    client_id varchar(36),
    client_version varchar(10),
    ip varchar(39),
    rsp_time timestamp NOT NULL,
    req_millis bigint NOT NULL,
    rsp_status int2 NOT NULL,
    rsp_body text
);

create index infra_request_log_req_id_idx on infra_request_log (req_id);
create index infra_request_log_req_time_idx on infra_request_log (req_time);
create index infra_request_log_api_idx on infra_request_log (api);
create index infra_request_log_tag_idx on infra_request_log (tag);
create index infra_request_log_tenant_id_idx on infra_request_log (tenant_id);
create index infra_request_log_principal_idx on infra_request_log (principal);
create index infra_request_log_req_millis_idx on infra_request_log (req_millis);

CREATE TABLE infra_error_log
(
    id uuid primary key,
    occur_at timestamp NOT NULL,
    error_code char(4) NOT NULL,
    error_msg text,
    req_id varchar(36),
    req_time timestamp,
    api varchar(255) NOT NULL,
    querystring text,
    req_body text,
    project varchar(20),
    function varchar(30),
    tag varchar(30),
    source varchar(30) NOT NULL,
    tenant_id varchar(20),
    principal varchar(36),
    run_as bool NOT NULL,
    client_id varchar(36),
    client_version varchar(10),
    ip varchar(39),
    rsp_time timestamp,
    req_millis bigint,
    service_name varchar(30),
    service_api varchar(255),
    service_rsp_code varchar(20),
    service_req_body text,
    service_rsp_body varchar(30)
);

create index infra_error_log_req_id_idx on infra_error_log (req_id);
create index infra_error_log_occur_at_idx on infra_error_log (occur_at);
create index infra_error_log_api_idx on infra_error_log (api);
create index infra_error_log_tag_idx on infra_error_log (tag);

CREATE TABLE infra_login_log
(
    id uuid primary key,
    req_time timestamp NOT NULL,
    result_code varchar(20) NOT NULL,
    user_id varchar(36) NOT NULL,
    project varchar(20) NOT NULL,
    source varchar(30) NOT NULL,
    tenant_id varchar(20),
    client_id varchar(36),
    client_version varchar(10),
    ip varchar(39),
    sid varchar(120)
);

create index infra_login_log_req_id_idx on infra_login_log (req_time);
create index infra_login_log_result_code_idx on infra_login_log (result_code);
create index infra_login_log_user_id_idx on infra_login_log (user_id);
create index infra_login_log_tenant_id_idx on infra_login_log (tenant_id);

CREATE TABLE infra_notification_channel_log
(
    id uuid primary key,
    type varchar(30) NOT NULL,
    channel int2 NOT NULL,
    recipients text NOT NULL,
    create_time timestamp NOT NULL,
    send_time timestamp NOT NULL,
    content text,
    success_list text,
    failure_list text,
    invalid_recipient_ids text,
    rsp_code varchar(30),
    rsp_msg text,
    rsp_time timestamp,
    rsp_body text
);

create index infra_notification_channel_log_type_idx on infra_notification_channel_log (type);
create index infra_notification_channel_log_channel_idx on infra_notification_channel_log (channel);
create index infra_notification_channel_log_send_time_idx on infra_notification_channel_log (send_time);
