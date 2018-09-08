Welcome to the Spok API documentation.
<br />You'll find below all the informations that you will need to build an awesome Spok application (mobile, touchpad, web or whatever!).

<hr />

<span id="basics"></span>
# Basics
The API entry point is <a href="https://api.spok.me" target=_blank>api.spok.me</a>.
<br />It provides mainly <a href="#websockets">websockets resources</a>, for writing ones, and sometimes <a href="#restful">RESTful resources</a>, for read-only ones.
<br />The encoding charset use for reading and writing data is <b>UTF-8</b>.
<br />The data format use for all the exchanges between the clien and the server is <b>JSON</b>.

<hr />

<span id="websockets"></span>
# Websockets resources
Data sent through sockets allow a reactive application.
<br />This is a push system that don't require any previous request.
<br />For example, the client UI could be updated without any interaction on its side, by a simple push of the required data through a specific channel.
<br />

The two most important data fields are:
- the `action` when client emit data, this is what the API will look first
- the `resource` when client receive data, to know what is it about (mostly similar to the `action`in a synchronous request-response context)

For each documented websocket, there's few important informations:
- description > "Must wait for the response": "yes"|"no".
<br />It can be read as "Is this websocket interaction must simulate a synchronous data exchange by waiting a response after sending the request?".
<br />It indicates if the client must wait the websocket to send back a resource response corresponding to the action request to acknowledge it (e.g. change of screen, displaying a message, ...) or not
- tabs > "Sample sent": sample of data sent by the user which expect at least an `action`
- tabs > "Sample received":  sample of data received by the user (see also the <a href="#response">response</a> section)
- "Parameter" cell: list of the fields sent by the client
- "Success 200" cell: list of the request-specific fields recevied by the client in case of sucess
- "Error 4xx": list of possible errors returned in case of failure

<hr />

<span id="restful"></span>
# RESTful resources
In a read-only context RESTful resources are mainly used to initialize a context, a page, a screen, by loading the already existing data (e.g. a comments list).
<br />They are flagged like this: <span class="label label-success">GET</span>

<hr />

<span id="http-codes"></span>
### HTTP codes
When a resource is requested, the global <a href="#response-structure-status">status</a> and the <a href="#response-structure-errors">errors container</a> helps to check if everything is fine or not.
<br />Another data is also available via the HTTP code.
<br />The HTTP code value can help to find out and understand what happened.
<br />
<br />In a case of a normal utilisation of the API, the HTTP codes returned will be:
<br />- <b>[200]</b> or <b>[202]</b>: if success
<br />- <b>[400]</b>: if failure
<br />- <b>[401]</b>: if the requested resource requires a connected user
<br />- <b>[402]</b>: if the requested resource is a paid access
<br />
<br />The table below describes which HTTP codes are used by the Spok API and in which case.
<table class="table">
    <tr><th>Code</th><th>State</th><th>Description</th></tr>
    <tr class="success text-success"><td>200</td><td>Nominal success case</td><td>Valid call to an API resource.</td></tr>
    <tr class="success text-success"><td>202</td><td>Nominal success case</td><td>Valid call to a deprecated API resource (will be removed in a later version of the API).</td></tr>
    <tr class="warning text-warning"><td>400</td><td>Nominal failure case</td><td>Valid call to an API resource, but the <a href="#response-structure-status">global status</a> is in failure.</td></tr>
    <tr class="warning text-warning"><td>401</td><td>Nominal failure case</td><td>Authentication required.</td></tr>
    <tr class="warning text-warning"><td>402</td><td>Nominal failure case</td><td>Payment required.</td></tr>
    <tr class="error text-error"><td>404</td><td>Error case</td><td>Resource not found.</td></tr>
    <tr class="error text-error"><td>429</td><td>Error case</td><td>Too many requests.</td></tr>
    <tr class="error text-error"><td>503</td><td>Error case</td><td>Generic error (failback).</td></tr>
</table>

<hr />

<span id="system-errors"></span>
### "System" errors
These are the errors than can occured with any (or almost) API resource called and that maps <a href="#http-codes">HTTP codes</a>.
<table>
    <tr><th>ID</th><th>HTTP code</th><th>Message</th></tr>
    <tr><td>SYST-400</td><td>400</td><td>Invalid call to the service <code>xxx</code> (wrong parameters).</td></tr>
    <tr><td>SYST-401</td><td>401</td><td>Authentication required..</td></tr>
    <tr><td>SYST-402</td><td>402</td><td>Payment required.</td></tr>
    <tr><td>SYST-404</td><td>404</td><td>Service <code>xxx</code> does not exist.</td></tr>
    <tr><td>SYST-429</td><td>429</td><td>Too many requests.</td></tr>
    <tr><td>SYST-503</td><td>503</td><td>Service unavailable.</td></tr>
</table>

<hr />

<span id="request"></span>
### Input requests
Unless an explicit "optionnal" flag, all input parameters listed within the resources are required.


<span id="response"></span>
### Output responses
All output fields listed within the resources are optionnal.
<br />Each call to the API will have the following structure response (JSON example)

    {
        resource: xxx,
        status: xxx,
        errors: [...],
        data: {...}
    }
<br />
<br />The <b>resource</b> is a <i>string</i> that represent the full path, parameters included, of the requested resource.
<br />
<br />The <b>status</b> is a <i>string</i> that represent the global status of the response.
<br />See "<a href="#lexicon-execution-statuses">status</a>" to view the possible values.
<br />See the <a href="#http-codes">http codes</a> to understand the associate http code returned.
<br />
<br />This <b>data</b> is an <i>object</i> that contain all the informations returned when calling a resource.
<br />If the global status of the request is not in success, the data are empty.

    data: {...}
<br />
<br />The <b>errors</b> container is an array of objects (<i>object[]</i>).
<br />Each error are represented by a unique identifier and a message.
<br />If there is potentially few fields that could be concerned by a same error and to avoid any misunderstanding, an optional "field" attribute can be added to specify the name of the invalid field. Name will reflect the one provided by the client.
<br />If the global status of the request is failed, the errors container is filled with detailled errors.
<br />In addition to the explicit and known errors, each RESTful resource has a generic error with a code ending with "10x".
<br />It's a kind of "catch-all" error to handle unexpected cases, if something wrong happened but is not clearly identified yet.

    errors: [
        {id:"ERR-001", message:"This is a sample an error's message."},
        {id:"ERR-002", message:"This is another sample an error's message.", field:"pwd_confirm"},
        ...
    ],

<hr />

<span id="data-types"></span>
## Data types
The types of the data describes in this documentation are the following:
<br />- <b>array</b>: container.
<br />- <b>boolean</b>: only 0 or 1. Values as "false", "true", "n", "y", ... are not valid.
<br />- <b>date</b>: calendar date based on a "YYYY-MM-DD" format.
<br />- <b>time</b>: 24h time format "hh:mm:ss" format.
<br />- <b>decimal</b>: for a decimal number, the split between the integer and the float part is the dot ".".
<br />- <b>integer</b>: integer number.
<br />- <b>object</b>: container.
<br />- <b>string</b>: string of UTF-8 encoded characters.
<br />- <b>timestamp</b>: UNIX-based timestamp.
<br />
<br />If a field is, for example, an array of integer, the notation will be: <i>integer[]</i>.
<br />If it is an array of object: <i>object[]</i>.

