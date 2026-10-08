-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
-- 失物领域结构；不插入虚构失物、失主或认领记录。
create table storage_spot (
 id bigint auto_increment primary key,
 department_id bigint not null,
 code varchar(80) not null,
 name varchar(200) not null,
 enabled boolean not null,
 version bigint not null,
 unique(department_id,code),
 foreign key(department_id) references department(id)
);

create table found_item (
 id bigint auto_increment primary key,
 department_id bigint not null,
 storage_id bigint not null,
 code varchar(80) not null,
 title varchar(200) not null,
 category varchar(80) not null,
 color varchar(80) not null,
 found_date date not null,
 found_place varchar(200) not null,
 private_marks varchar(2000) not null,
 note varchar(2000) not null,
 received_date date not null,
 retain_until date not null,
 retention_days integer not null,
 status varchar(80) not null,
 created_by bigint not null,
 created_at timestamp(6) not null,
 version bigint not null,
 unique(code),
 foreign key(department_id) references department(id),
 foreign key(storage_id) references storage_spot(id),
 foreign key(created_by) references account(id)
);

create table lost_report (
 id bigint auto_increment primary key,
 department_id bigint not null,
 reporter_id bigint not null,
 title varchar(200) not null,
 category varchar(80) not null,
 color varchar(80) not null,
 lost_date date not null,
 lost_place varchar(200) not null,
 description varchar(2000) not null,
 status varchar(80) not null,
 created_at timestamp(6) not null,
 version bigint not null,
 foreign key(department_id) references department(id),
 foreign key(reporter_id) references account(id)
);

create table claim_case (
 id bigint auto_increment primary key,
 department_id bigint not null,
 item_id bigint not null,
 report_id bigint not null,
 claimant_id bigint not null,
 proposed_by bigint not null,
 reviewed_by bigint,
 handed_by bigint,
 status varchar(80) not null,
 message varchar(2000) not null,
 item_title varchar(200) not null,
 item_marks varchar(2000) not null,
 report_title varchar(200) not null,
 report_description varchar(2000) not null,
 evidence varchar(2000) not null,
 review_note varchar(2000) not null,
 handover_note varchar(2000) not null,
 receipt_note varchar(2000) not null,
 created_at timestamp(6) not null,
 submitted_at timestamp(6),
 reviewed_at timestamp(6),
 pickup_until timestamp(6),
 handed_at timestamp(6),
 received_at timestamp(6),
 closed_at timestamp(6),
 version bigint not null,
 foreign key(department_id) references department(id),
 foreign key(item_id) references found_item(id),
 foreign key(report_id) references lost_report(id),
 foreign key(claimant_id) references account(id),
 foreign key(proposed_by) references account(id),
 foreign key(reviewed_by) references account(id),
 foreign key(handed_by) references account(id)
);

create table disposal_case (
 id bigint auto_increment primary key,
 department_id bigint not null,
 item_id bigint not null,
 requested_by bigint not null,
 reviewed_by bigint,
 executed_by bigint,
 method varchar(80) not null,
 reason varchar(2000) not null,
 status varchar(80) not null,
 review_note varchar(2000) not null,
 execution_note varchar(2000) not null,
 created_at timestamp(6) not null,
 reviewed_at timestamp(6),
 executed_at timestamp(6),
 version bigint not null,
 foreign key(department_id) references department(id),
 foreign key(item_id) references found_item(id),
 foreign key(requested_by) references account(id),
 foreign key(reviewed_by) references account(id),
 foreign key(executed_by) references account(id)
);

create table custody_event (
 id bigint auto_increment primary key,
 department_id bigint not null,
 item_id bigint,
 report_id bigint,
 claim_id bigint,
 disposal_id bigint,
 actor_id bigint not null,
 action varchar(80) not null,
 note varchar(2000) not null,
 created_at timestamp(6) not null,
 foreign key(department_id) references department(id),
 foreign key(item_id) references found_item(id),
 foreign key(report_id) references lost_report(id),
 foreign key(claim_id) references claim_case(id),
 foreign key(disposal_id) references disposal_case(id),
 foreign key(actor_id) references account(id)
);

create table photo_asset (
 id bigint auto_increment primary key,
 item_id bigint,
 report_id bigint,
 created_by bigint not null,
 created_at timestamp(6) not null,
 sha256 varchar(80) not null,
 active boolean not null,
 version bigint not null,
 content longblob not null,
 check ((item_id is null) <> (report_id is null)),
 foreign key(item_id) references found_item(id),
 foreign key(report_id) references lost_report(id),
 foreign key(created_by) references account(id)
);

create index idx_item_site_state on found_item(department_id,status);
create index idx_report_owner on lost_report(reporter_id,status);
create index idx_claim_item on claim_case(item_id,status);
create index idx_claim_report on claim_case(report_id,status);
create index idx_disposal_item on disposal_case(item_id,status);
