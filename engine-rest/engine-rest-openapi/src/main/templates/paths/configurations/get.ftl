<#macro endpoint_macro docsUrl="">
{
  <@lib.endpointInfo
      id = "getConfigurations"
      tag = "Configuration"
      summary = "Get Configurations"
      desc = "Retrieves configurations for a tenant scope.

              If `tenantId` is omitted or blank, global default configurations are returned.
              By default, only configurations with status `ACTIVE` are returned. Set
              `includeInactive` to `true` to include inactive configurations."
  />

  "parameters": [

    <@lib.parameter
        name = "tenantId"
        location = "query"
        type = "string"
        desc = "The tenant scope of the configurations to retrieve. If omitted or blank, retrieves global defaults." />

    <@lib.parameter
        name = "includeInactive"
        location = "query"
        type = "boolean"
        defaultValue = "false"
        last = true
        desc = "Whether to include inactive configurations. By default, only active configurations are returned." />
  ],

  "responses": {

    <@lib.response
        code = "200"
        dto = "ConfigurationDto"
        array = true
        desc = "Request successful."
        examples = ['"global-configurations": {
                       "summary": "GET `/configurations`",
                       "value": [
                         {
                           "id": "8a6cf4aa-6db5-4b95-9871-1f0f9ef8b2f5",
                           "configKey": "mail.from",
                           "configValue": "noreply@example.org",
                           "version": 1,
                           "status": "ACTIVE",
                           "createdBy": "demo",
                           "createdAt": "2026-08-20T14:00:00.000+0000",
                           "updatedBy": "demo",
                           "updatedAt": "2026-08-20T14:00:00.000+0000"
                         }
                       ]
                     }',
                    '"tenant-configurations": {
                       "summary": "GET `/configurations?tenantId=tenant-a&includeInactive=true`",
                       "value": [
                         {
                           "id": "8a6cf4aa-6db5-4b95-9871-1f0f9ef8b2f5",
                           "configKey": "mail.from",
                           "tenantId": "tenant-a",
                           "configValue": "noreply@example.org",
                           "version": 1,
                           "status": "ACTIVE",
                           "createdBy": "demo",
                           "createdAt": "2026-08-20T14:00:00.000+0000",
                           "updatedBy": "demo",
                           "updatedAt": "2026-08-20T14:00:00.000+0000"
                         },
                         {
                           "id": "bf77b760-4b6e-40a7-a3ad-0c8d026270c8",
                           "configKey": "legacy.mail.from",
                           "tenantId": "tenant-a",
                           "configValue": "old@example.org",
                           "version": 2,
                           "status": "DELETED",
                           "createdBy": "demo",
                           "createdAt": "2026-08-19T14:00:00.000+0000",
                           "updatedBy": "demo",
                           "updatedAt": "2026-08-21T14:00:00.000+0000"
                         }
                       ]
                     }']
    />

    <@lib.response
        code = "403"
        dto = "ExceptionDto"
        desc = "Returned if the request is unauthenticated, a tenant user requests global configurations,
                or the requested tenant is outside the user's authenticated tenant scope."
        last = true
    />
  }
}
</#macro>
