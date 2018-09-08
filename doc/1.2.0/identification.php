<?php


class Identification
{
    /**
     * @apiVersion 1.0.0
     * @apiGroup Identification
     * @apiName register (step 1 - phone)
     * @api {websocket} / register (step 1 - phone)
     * @apiDescription <b>Must wait for the response: <code>yes</code></b>.
     * <br />Service to create a new account.
     * <br />This is the first step of a three-steps operation:
     * - <b>step 1</b>: enter a phone number to be sent a SMS confirmation code
     * - <a href="#api-Identification-register__step_2___code_">step 2</a>: enter the SMS confirmation code to link the app to the phone
     * - <a href="#api-Identification-register__step_3___details_">step 3</a>: enter details
     * 
     * 
     * @apiExample {data} Sample sent
     * {
     *   action: "register",
     *   country_code: "+33",
     *   phone_number: "123456789"
     * }
     * 
     * @apiExample {data} Sample received
     * {
     *   data: "OTP has been sent to +33123456789", // in case of success
     *   errors: [...],
     *   resource: "register",
     *   status: "success"|"failed"
     * }
     * 
     * @apiParam    {string}    action          <code>register</code>
     * @apiParam    {string}    country_code    User's <a href="https://en.wikipedia.org/wiki/List_of_country_calling_codes" target=_blank>country calling code</a>.
     * @apiParam    {string}    phone_number    User's phone number (without country code neither leading 0).
     *
     * @apiSuccess  {string}    data        Sucessful message.
     * @apiSuccess  {object[]}  errors      Errors list.
     * @apiSuccess  {string}    resource    <code>register</code>
     * @apiSuccess  {string}    status      Operation's <a href="#response-structure-status">status</a>.
     *
     * @apiUse RGX002
     * @apiUse IDT005
     * @apiUse IDT103
     *
     */
    

    /**
     * @apiVersion 1.0.0
     * @apiGroup Identification
     * @apiName register (step 2 - code)
     * @api {websocket} / register (step 2 - code)
     * @apiDescription <b>Must wait for the response: <code>yes</code></b>.
     * <br />Service to create a new account.
     * <br />This is the second step of a three-steps operation:
     * - <a href="#api-Identification-register__step_1___phone_">step 1</a>: enter a phone number to be sent a SMS confirmation code
     * - <b>step 2</b>: enter the SMS confirmation code to link the app to the phone
     * - <a href="#api-Identification-register__step_3___details_">step 3</a>: enter details
     * 
     * 
     * @apiExample {data} Sample sent
     * {
     *   action: "code",
     *   code: "1234",
     *   phone_number: "+33123456789"
     * }
     * 
     * @apiExample {data} Sample received
     * {
     *   data: "+33123456789", // in case of success
     *   errors: [...],
     *   resource: "code",
     *   status: "success"|"failed"
     * }
     * 
     * @apiParam    {string}    action          <code>code</code>
     * @apiParam    {string}    code            SMS-sent code.
     * @apiParam    {string}    phone_number    User's <a href="#lexicon-phone-number-format">phone number</a>.
     *
     * @apiSuccess  {string}    data        Sucessful message.
     * @apiSuccess  {object[]}  errors      Errors list.
     * @apiSuccess  {string}    resource    <code>register</code>
     * @apiSuccess  {string}    status      Operation's <a href="#response-structure-status">status</a>.
     *
     * @apiUse RGX002
     * @apiUse IDT005
     * @apiUse IDT103
     */
    public function registerStep2() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Identification
     * @apiName register (step 3 - details)
     * @api {websocket} / register (step 3 - details)
     * @apiDescription <b>Must wait for the response: <code>yes</code></b>.
     * <br />Service to create a new account.
     * <br />A <a href="#api-Identification-contact_s_registered">"contact's registered"</a> event is sent and a new "registration" <a href="#api-My_Account-notifications">notifications</a> is added.
     * <br />This is the third step of a three-steps operation:
     * - <a href="#api-Identification-register__step_1___phone_">step 1</a>: enter a phone number to be sent a SMS confirmation code
     * - <a href="#api-Identification-register__step_2___code_">step 2</a>: enter the SMS confirmation code to link the app to the phone
     * - <b>step 3</b>: enter details
     * <br />
     * 
     * 
     * 
     * @apiExample {data} Sample sent
     *  {
     *    "action":"details",
     *    "birthdate": "1979-05-04",
     *    "contacts":["+33987654321", "+33963852741"],
     *    "gender":"male",
     *    "location": {...},
     *    "nickname": "cyril",
     *    "phone_number":"+33123456789"
     * }
     * 
     * @apiExample {data} Sample received
     * {
     *   "data": {
     *     "contacts_ids": [userContactsIds]
     *     "token": "1ab2c34d/5e+6f7g",
     *     "user_id": "9h6g-1s3f-5a9l",
     *   },
     *   errors: [...],
     *   resource: "code",
     *   status: "success"|"failed"
     * }
     * 
     * @apiParam    {string}    action          <code>details</code>
     * @apiParam    {date}      birthdate       User's birthdate. There's some restriction regarding user's <a href="#lexicon-users-ages">age</a>.
     * @apiParam    {string[]}  contacts        Contact's <a href="#lexicon-phone-number-format">phone numbers</a>.
     * @apiParam    {string}    gender          User's <a href="#lexicon-users-genders">gender</a>.
     * @apiParam    {string}    location        User's living town <a href="#lexicon-googlemap-address">GoogleMap location</a>.
     * @apiParam    {string}    nickname        User's <a href="#lexicon-nickname-format">nickname</a>.
     * @apiParam    {string}    phone_number    User's <a href="#lexicon-phone-number-format">phone number</a>.
     *
     * @apiSuccess  {string}    data                Sucessful message.
     * @apiSuccess  {string[]}  data.contacts_ids   ???
     * @apiSuccess  {string}    data.token          Authentication token (to be sent for any API request requiring authentication).
     * @apiSuccess  {string}    data.user_id        User identifier.
     * @apiSuccess  {object[]}  errors              Errors list.
     * @apiSuccess  {string}    resource            <code>register</code>
     * @apiSuccess  {string}    status              Operation's <a href="#response-structure-status">status</a>.
     * 
     * @apiUse RGX003
     * @apiUse RGX004
     * @apiUse RGX005
     * @apiUse TIME008
     * @apiUse IDT104
     */
    public function registerStep3() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Identification
     * @apiName unregister
     * @api {websocket} /unregister unregister
     * @apiDescription Service to unregister a phone.
     * <br />At the end of the operation, user is disconnected from the app (token removed).
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {string}    phone_number  <a href="#lexicon-phone-number-format">Phone number</a> (should be identical to the action's launcher).
     * 
     * @apiUse RGX001
     * @apiUse IDT002
     * @apiUse IDT003
     * @apiUse IDT007
     * @apiUse IDT105
     */
    public function unregister() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Identification
     * @apiName authenticate
     * @api {websocket} /authenticate authenticate
     * @apiDescription Service to authenticate a user.
     * <br />This is not an user explicit action, but this action will be launched each time the application starts.
     * 
     * @apiParam    {string}    phone_number  <a href="#lexicon-phone-number-format">Phone number</a>.
     * 
     * @apiSuccess  {string}    token   Authentication token.
     *
     * @apiUse IDT002
     * @apiUse IDT003
     * @apiUse IDT007
     * @apiUse IDT101
     */
    public function authenticate() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Identification
     * @apiName update phone (step 1)
     * @api {websocket} /my/settings/phone update phone (step 1)
     * @apiDescription Service to change the phone number.
     * <br /><b>step 1</b>: old and new mobile numbers are entered, a confirmation code is sent by SMS to the new one.
     * <br /><a href="#api-Identification-update_phone__step_2_">step 2</a>: confirmation code for the new mobile is entered.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {string}    old_number  Current used <a href="#lexicon-phone-number-format">phone number</a>.
     * @apiParam    {string}    new_number  New <a href="#lexicon-phone-number-format">phone number</a>.
     * 
     * @apiUse RGX001
     * @apiUse IDT004
     * @apiUse IDT106
     */
    public function updatePhoneNumberStep1() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Identification
     * @apiName update phone (step 2)
     * @api {websocket} /my/settings/phone/code update phone (step 2)
     * @apiDescription Service to change the phone number.
     * <br /><a href="#api-Identification-update_phone__step_1_">step 1</a>: old and new mobile numbers are entered, a confirmation code is sent to the new one.
     * <br /><b>step 2</b>: confirmation code for the new mobile is entered.
     * <br />At the end of the operation, user is disconnected from the app (token removed).
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {string}    phone_number    User new <a href="#lexicon-phone-number-format">phone number</a>.
     * @apiParam    {string}    code            Confirmation code (SMS sent).
     * 
     * @apiUse RGX002
     * @apiUse IDT107
     */
    public function updatePhoneNumberStep2() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Identification
     * @apiName support
     * @api {websocket} /contact support
     * @apiDescription Service to contact the support.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam   {string}    message Text of the message to be sent to the support.
     * 
     * @apiUse IDT008
     * @apiUse IDT108
     */
    public function contactSupport() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Identification
     * @apiName contact's registered
     * @api {websocket} / contact's registered
     * @apiDescription Event emitted when one of my phone's contact <a href="#api-Identification-register__step_3___details_">registered</a> to this application.
     * <br />Event received any registered user that have this new incoming user in its phone's contacts.
     * 
     * @apiUse AuthRequired
     * 
     * 
     * @apiUse EventUserStructure
     */
    public function contactRegistered() { return; }
}
