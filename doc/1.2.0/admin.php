<?php

/**
 * @apiDescription Should be IP-based restricted or oAuth / gmail (as Chris have done for our internal tools)
 * 
 */
class Admin
{
    /**
     * @apiVersion 1.0.0
     * @apiGroup Admin
     * @apiName something
     * @api {get} /admin/something something
     * @apiDescription Service to get something.
     * 
     * @apiUse AdminAuthRequired
     * 
     */
    public function getSomething() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Admin
     * @apiName translation
     * @api {websocket} /translation translation
     * @apiDescription Service to alert for a missing translation.
     * <br />An email is sent to the admin of the application.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {string}    text    Text to be translated.
     * @apiParam    {string}    lang    "fr" | "en" | "de" | "es" | "it". Lang for the text to be translated in.
     * 
     */
    public function askForTranslation() { return; }
}