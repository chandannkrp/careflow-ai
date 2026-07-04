create table knowledge_document (
    id uuid primary key,
    title varchar(255) not null,
    file_name varchar(255) not null,
    content text not null,
    embedding text not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_knowledge_document_updated_at on knowledge_document (updated_at);
