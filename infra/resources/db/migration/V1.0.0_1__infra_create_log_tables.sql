CREATE TABLE infra_request_log
(
    id uuid primary key,
    req_id varchar(36) NOT NULL,
    req_at timestamp NOT NULL,
    api varchar(255) NOT NULL,
    headers text,
    querystring text,
    req_body text,
    project varchar(20) NOT NULL,
    function varchar(30) NOT NULL,
    tag varchar(36),
    source varchar(30) NOT NULL,
    tenant_id varchar(20),
    principal varchar(64) NOT NULL,
    run_as bool NOT NULL,
    client_id varchar(36),
    client_version varchar(10),
    ip varchar(39),
    rsp_at timestamp NOT NULL,
    rsp_time integer NOT NULL,
    rsp_status int2 NOT NULL,
    rsp_body text
);

create index infra_request_log_req_id_idx on infra_request_log (req_id);
create index infra_request_log_req_at_idx on infra_request_log (req_at);
create index infra_request_log_api_idx on infra_request_log (api);
create index infra_request_log_tag_idx on infra_request_log (tag);
create index infra_request_log_tenant_id_idx on infra_request_log (tenant_id);
create index infra_request_log_principal_idx on infra_request_log (principal);
create index infra_request_log_rsp_time_idx on infra_request_log (rsp_time);

CREATE TABLE infra_error_log
(
    id uuid primary key,
    occur_at timestamp NOT NULL,
    error_code char(4) NOT NULL,
    error_code_type varchar(20) NOT NULL,
    error_msg text,
    data text,
    req_id varchar(36),
    req_at timestamp,
    api varchar(255),
    headers text,
    querystring text,
    req_body text,
    project varchar(20),
    function varchar(30),
    tag varchar(36),
    source varchar(30) NOT NULL,
    tenant_id varchar(20),
    principal varchar(64),
    run_as bool NOT NULL,
    client_id varchar(36),
    client_version varchar(10),
    ip varchar(39),
    rsp_at timestamp,
    rsp_time integer,
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
create index infra_error_log_error_code_idx on infra_error_log (error_code);
create index infra_error_log_error_code_type_idx on infra_error_log (error_code_type);

CREATE TABLE infra_login_log
(
    id uuid primary key,
    user_id uuid NOT NULL,
    result_code varchar(20) NOT NULL,
    occur_at timestamp NOT NULL,
    project varchar(20) NOT NULL,
    source varchar(30) NOT NULL,
    tenant_id varchar(20),
    client_id varchar(36),
    client_version varchar(10),
    ip varchar(39),
    sid varchar(120)
);

create index infra_login_log_user_id_idx on infra_login_log (user_id);
create index infra_login_log_result_code_idx on infra_login_log (result_code);
create index infra_login_log_occur_at_idx on infra_login_log (occur_at);
create index infra_login_log_tenant_id_idx on infra_login_log (tenant_id);

CREATE TABLE infra_notification_message_log
(
    id uuid primary key,
    notification_id uuid NOT NULL,
    event_id uuid NOT NULL,
    type varchar(30) NOT NULL,
    version varchar(5),
    channel varchar(20) NOT NULL,
    lang varchar(20) NOT NULL,
    send_at timestamp,
    error_msg text,
    receivers text NOT NULL,
    content text,
    success  bool NOT NULL,
    success_list text,
    failure_list text,
    invalid_recipient_ids text,
    rsp_code varchar(30),
    rsp_msg text,
    rsp_at timestamp,
    rsp_time integer,
    rsp_body text
);

create index infra_notification_log_notification_id_idx on infra_notification_message_log (notification_id);
create index infra_notification_log_event_id_idx on infra_notification_message_log (event_id);
create index infra_notification_log_type_idx on infra_notification_message_log (type);
create index infra_notification_log_channel_idx on infra_notification_message_log (channel);
create index infra_notification_log_send_at_idx on infra_notification_message_log (send_at);
