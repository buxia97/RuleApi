# 基础介绍

| 说明     | 内容 |
| :------- | ---- |
| 项目名   | RuleApi |
| 作者     | buxia97 |
| 数据库IP | 127.0.0.1 |
| 数据库名 | typechoapi |

## typecho_comments表结构说明
| 代码字段名 | 字段名 | 数据类型（代码） | 数据类型 | 长度 | NullAble | 注释 |
| :--------- | ------ | ---------------- | -------- | ---- | -------------- | ---- |
| coid | coid | Integer | int | 10 | NO |  |
| cid | cid | Integer | int | 10 | YES |  |
| created | created | Integer | int | 10 | YES |  |
| author | author | String | varchar | 200 | YES |  |
| authorId | authorId | Integer | int | 10 | YES |  |
| ownerId | ownerId | Integer | int | 10 | YES |  |
| mail | mail | String | varchar | 200 | YES |  |
| url | url | String | varchar | 200 | YES |  |
| ip | ip | String | varchar | 64 | YES |  |
| agent | agent | String | varchar | 200 | YES |  |
| text | text | String | text | 65535 | YES |  |
| type | type | String | varchar | 16 | YES |  |
| status | status | String | varchar | 16 | YES |  |
| parent | parent | Integer | int | 10 | YES |  |

## typecho_contents表结构说明
| 代码字段名 | 字段名 | 数据类型（代码） | 数据类型 | 长度 | NullAble | 注释 |
| :--------- | ------ | ---------------- | -------- | ---- | -------------- | ---- |
| cid | cid | Integer | int | 10 | NO |  |
| title | title | String | varchar | 200 | YES |  |
| slug | slug | String | varchar | 200 | YES |  |
| created | created | Integer | int | 10 | YES |  |
| modified | modified | Integer | int | 10 | YES |  |
| text | text | String | longtext | 4294967295 | YES |  |
| orderKey | order | Integer | int | 10 | YES |  |
| authorId | authorId | Integer | int | 10 | YES |  |
| template | template | String | varchar | 32 | YES |  |
| type | type | String | varchar | 16 | YES |  |
| status | status | String | varchar | 16 | YES |  |
| password | password | String | varchar | 32 | YES |  |
| commentsNum | commentsNum | Integer | int | 10 | YES |  |
| allowComment | allowComment | String | char | 1 | YES |  |
| allowPing | allowPing | String | char | 1 | YES |  |
| allowFeed | allowFeed | String | char | 1 | YES |  |
| parent | parent | Integer | int | 10 | YES |  |

## typecho_fields表结构说明
| 代码字段名 | 字段名 | 数据类型（代码） | 数据类型 | 长度 | NullAble | 注释 |
| :--------- | ------ | ---------------- | -------- | ---- | -------------- | ---- |
| cid | cid | Integer | int | 10 | NO |  |
| name | name | String | varchar | 200 | NO |  |
| type | type | String | varchar | 8 | YES |  |
| strValue | str_value | String | text | 65535 | YES |  |
| intValue | int_value | Integer | int | 10 | YES |  |
| floatValue | float_value | Float | float | 12 | YES |  |

## typecho_metas表结构说明
| 代码字段名 | 字段名 | 数据类型（代码） | 数据类型 | 长度 | NullAble | 注释 |
| :--------- | ------ | ---------------- | -------- | ---- | -------------- | ---- |
| mid | mid | Integer | int | 10 | NO |  |
| name | name | String | varchar | 200 | YES |  |
| slug | slug | String | varchar | 200 | YES |  |
| type | type | String | varchar | 32 | NO |  |
| description | description | String | varchar | 200 | YES |  |
| count | count | Integer | int | 10 | YES |  |
| orderKey | order | Integer | int | 10 | YES |  |
| parent | parent | Integer | int | 10 | YES |  |

## typecho_relationships表结构说明
| 代码字段名 | 字段名 | 数据类型（代码） | 数据类型 | 长度 | NullAble | 注释 |
| :--------- | ------ | ---------------- | -------- | ---- | -------------- | ---- |
| cid | cid | Integer | int | 10 | NO |  |
| mid | mid | Integer | int | 10 | NO |  |

## typecho_users表结构说明
| 代码字段名 | 字段名 | 数据类型（代码） | 数据类型 | 长度 | NullAble | 注释 |
| :--------- | ------ | ---------------- | -------- | ---- | -------------- | ---- |
| uid | uid | Integer | int | 10 | NO |  |
| name | name | String | varchar | 32 | YES |  |
| password | password | String | varchar | 64 | YES |  |
| mail | mail | String | varchar | 200 | YES |  |
| url | url | String | varchar | 200 | YES |  |
| screenName | screenName | String | varchar | 32 | YES |  |
| created | created | Integer | int | 10 | YES |  |
| activated | activated | Integer | int | 10 | YES |  |
| logged | logged | Integer | int | 10 | YES |  |
| groupKey | group | String | varchar | 16 | YES |  |
| authCode | authCode | String | varchar | 64 | YES |  |








