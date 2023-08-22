alter table organization add column kdm_center_id int;
alter table organization add foreign key(kdm_center_id) references kdm_center(id);
alter table organization add column latitude float4;
alter table organization add column longitude float4;
alter table organization add column import_issue varchar(1024);
