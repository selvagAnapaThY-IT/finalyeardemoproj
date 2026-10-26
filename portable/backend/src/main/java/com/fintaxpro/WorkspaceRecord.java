package com.fintaxpro;
import jakarta.persistence.*;
@Entity @Table(name="financial_workspaces") public class WorkspaceRecord{@Id String userId;@Column(columnDefinition="text",nullable=false) String payload="null";@Version long version;protected WorkspaceRecord(){}WorkspaceRecord(String userId){this.userId=userId;}}
