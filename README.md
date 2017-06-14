# amls-notification

**amls-notification** allows retrieval of secure notification messages, as well as creating new ones for a particular AMLS Registration Number.

## Usage

### Creating a new message

Send a POST request in JSON format to `/amls-notification/<reg number>` in order to create a new notification for the specified registration number.

#### Fields
| Name | Type | Description | Required | Validation |
| ----- | :------: | ----- | :-------: | ------ |
| name | string | The name of the recipient | Yes | Max 140 characters
| email | string | The recipient's email address | Yes | Max 100 characters
| variation | boolean | | Yes | Must be `true` or `false`
| status | Status | The status of the recipient's application (see below) | No | 
| contact_type | string | A 4-character string indicating the contact type | No | One of "REJR", "REVR", "CONA", "MTRJ", "NMRJ", "MTRV", "NMRV" "OTHR" |
| contact_number | string | A contact telephone number | No | Max 12 numbers between 0 and 9

**Status** is a subtype that has the following fields:

| Name | Type | Description | Required | Validation |
| ----- | :------: | ----- | :-------: | ------- |
| status_type | string | A two-digit status indicator | Yes | Must be one of "04", "06", "08", "10", "11"
| status_reason | string | A two-digit reason indicator | No | Must be two digits between "00" and "99"

#### Responses

| Status | Description | Body |
| :----: | ---- | ---- |
| 200 | The notification was created successfully | Content type of `application/json` with body content of `true` |
| 400 | The service was sent an invalid JSON request | Content type of `application/json` with a error packet containing validation errors (see below)|
| 400 | The service was sent an invalid AMLS Reference number | Content type of `application/json` with an error packet (see below) |

##### Error packet

In the cases where the service returns an error status, an error packet will be returned containing the following fields:

| Name | Type | Description |
| ---- | :----: | ------------|
| errors | String or Array | This will contain a single string as the error message, or an array of validation errors | 

##### Error packet example
```
{
  "errors": [
    { "path":"name", "error":"Too long" },
    { "path":"email", "error":"Too long" }
  ]
}

or

{
  "errors": "This is an example error message"
}
```

#### Example JSON request

```json
POST to /amls-notification/XJML00000000000

Content-Type: application/json

{
  "name":"Test",
  "email":"test@test.com",
  "status": {
   "status_type": "06",
    "status_reason": "01"
  },
  "contact_type": "REVR",
  "contact_number": "123456789",
  "variation": true
}
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
