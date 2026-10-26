import { integer, sqliteTable, text } from 'drizzle-orm/sqlite-core';
export const workspaces=sqliteTable('workspaces',{userId:text('user_id').primaryKey(),payload:text('payload').notNull(),version:integer('version').notNull().default(0),updatedAt:text('updated_at').notNull()});
