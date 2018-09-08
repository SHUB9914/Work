<?php


class Identification
{
    /**
     * @apiVersion 1.0.0
     * @apiGroup Identification
     * @apiName register (step 1)
     * @api {websocket} /register register (step 1)
     * @apiDescription Service to create a new account.
     * <br />This is the first step of a three-steps operation plus 2 bonus steps:
     * - <b>step 1</b>: enter a phone number to be sent a SMS confirmation code
     * - <a href="#api-Identification-register__step_2_">step 2</a>: enter the SMS confirmation code to link the app to the phone
     * - <a href="#api-Identification-register__step_3_">step 3</a>: enter details
     * - <a href="#api-Identification-register__step_FB_">step FB</a>: enter FB credential to pre-fill the profile with FB informations and FB contacts
     * - <a href="#api-Identification-invite">step invite friends</a>: invite phone's contacts and FB's friends
     * 
     * @apiParam    {string}    phone_number    User's <a href="#lexicon-phone-number-format">phone number</a>.
     *
     * @apiUse RGX001
     * @apiUse IDT001
     * @apiUse IDT102
     *
     */
    public function registerStep1() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Identification
     * @apiName register (step 2)
     * @api {websocket} /register/code register (step 2)
     * @apiDescription Service to create a new account.
     * <br />This is the second step of a three-steps operation plus 2 bonus steps:
     * - <a href="#api-Identification-register__step_1_">step 1</a>: enter a phone number to be sent a SMS confirmation code
     * - <b>step 2</b>: enter the SMS confirmation code to link the app to the phone
     * - <a href="#api-Identification-register__step_3_">step 3</a>: enter details
     * - <a href="#api-Identification-register__step_FB_">step FB</a>: enter FB credential to pre-fill the profile with FB informations and FB contacts
     * - <a href="#api-Identification-invite">step invite friends</a>: invite phone's contacts and FB's friends
     * 
     * @apiParam    {string}    phone_number    User's <a href="#lexicon-phone-number-format">phone number</a>.
     * @apiParam    {string}    code            SMS-sent code.
     *
     * @apiUse RGX002
     * @apiUse IDT005
     * @apiUse IDT103
     */
    public function registerStep2() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Identification
     * @apiName register (step 3)
     * @api {websocket} /register/details register (step 3)
     * @apiDescription Service to create a new account.
     * <br />A <a href="#api-Identification-contact_s_registered">"contact's registered"</a> event is sent and a new "registration" <a href="#api-My_Account-notifications">notifications</a> is added.
     * <br />This is the third step of a three-steps operation plus 2 bonus steps:
     * - <a href="#api-Identification-register__step_1_">step 1</a>: enter a phone number to be sent a SMS confirmation code
     * - <a href="#api-Identification-register__step_2_">step 2</a>: enter the SMS confirmation code to link the app to the phone
     * - <b>step 3</b>: enter details
     * - <a href="#api-Identification-register__step_FB_">step FB</a>: enter FB credential to pre-fill the profile with FB informations and FB contacts
     * - <a href="#api-Identification-invite">step invite friends</a>: invite phone's contacts and FB's friends
     * <br />
     * 
     * @apiParam    {string}    nickname    User's <a href="#lexicon-nickname-format">nickname</a>.
     * @apiParam    {date}      birthdate   User's <a href="#lexicon-users-ages">birthdate</a>.
     * @apiParam    {string}    location    User's living town location.
     * @apiParam    {string}    gender      User's <a href="#lexicon-users-genders">gender</a>.
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
     * @apiName register (step FB)
     * @api {websocket} /register/fb register (step FB)
     * @apiDescription Service to use a FB account to pre-fill details and load FB contacts.
     * <br />This is a bonus step of a three-steps operation plus 2 bonus steps:
     * - <a href="#api-Identification-register__step_1_">step 1</a>: enter a phone number to be sent a SMS confirmation code
     * - <a href="#api-Identification-register__step_2_">step 2</a>: enter the SMS confirmation code to link the app to the phone
     * - <a href="#api-Identification-register__step_3_">step 3</a>: enter details
     * - <b>step FB</b>: enter FB credential to pre-fill the profile with FB informations and FB contacts
     * - <a href="#api-Identification-invite">step invite friends</a>: invite phone's contacts and FB's friends
     * 
     *
     */
    public function registerStepFB() { return; }
    
    
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
     * @apiName invite
     * @api {websocket} /invite invite
     * @apiDescription Service to invite phone's contacts and FB's friend using the Spok app.
     * <br />Contacts and friends already invited can be re-invited only one month later).
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {string[]}  phone_numbers   List of contact's <a href="#lexicon-phone-number-format">phone numbers</a> to be invited (20 max at once)
     * @apiParam    {integer[]} fb_friends      List of FB's friends to be invited
     * 
     * @apiUse IDT009
     * @apiUse IDT109
     */
    public function inviteFriends() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Identification
     * @apiName contact's registered
     * @api {websocket} / contact's registered
     * @apiDescription Event emitted when one of my phone's contact <a href="#api-Identification-register__step_3_">registered</a> to this application.
     * <br />Event received any registered user that have this new incoming user in its phone's contacts.
     * 
     * @apiUse AuthRequired
     * 
     * 
     * @apiUse EventUserStructure
     */
    public function contactRegistered() { return; }
}
