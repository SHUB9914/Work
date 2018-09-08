# Lexicon
This is the Spok API lexicon.
<br />It describes any possible values or its format for all API fields.


<span id="lexicon-execution-statuses"></span>
### Execution statuses
Execution status are: "success", "failed".


<span id="lexicon-googlemap-address"></span>
### GoogleMap address
An address retrieved from GoogleMap is formatted as below (sample of the "results" field from http://maps.googleapis.com/maps/api/geocode/json?address=bastille&sensor=false):

    {
       "address_components" : [
          {
             "long_name" : "Place de la Bastille",
             "short_name" : "Place de la Bastille",
             "types" : [ "route" ]
          },
          {
             "long_name" : "Paris",
             "short_name" : "Paris",
             "types" : [ "locality", "political" ]
          },
          {
             "long_name" : "Paris",
             "short_name" : "Paris",
             "types" : [ "administrative_area_level_2", "political" ]
          },
          {
             "long_name" : "Île-de-France",
             "short_name" : "Île-de-France",
             "types" : [ "administrative_area_level_1", "political" ]
          },
          {
             "long_name" : "France",
             "short_name" : "FR",
             "types" : [ "country", "political" ]
          }
       ],
       "formatted_address" : "Place de la Bastille, Paris, France",
       "geometry" : {
          "bounds" : {
             "northeast" : {
                "lat" : 48.8538045,
                "lng" : 2.3705269
             },
             "southwest" : {
                "lat" : 48.8525007,
                "lng" : 2.3679415
             }
          },
          "location" : {
             "lat" : 48.8530778,
             "lng" : 2.3695501
          },
          "location_type" : "GEOMETRIC_CENTER",
          "viewport" : {
             "northeast" : {
                "lat" : 48.8545015802915,
                "lng" : 2.370583180291502
             },
             "southwest" : {
                "lat" : 48.8518036197085,
                "lng" : 2.367885219708498
             }
          }
       },
       "place_id" : "ChIJ-7wHAAFy5kcRhFlEb6qlhuc",
       "types" : [ "route" ]
    }


<span id="lexicon-nickname-format"></span>
### Nickname format
Nickname can be a pseudonym or contains the forename + name.
Nickname must follow the following rules:
- 3-80 alphabetic chars
- space, dash "-", simple quotes "'", double quote '"' and point "." are allowed, but not as start
- case should not be altered
- nickname is not unique


<span id="lexicon-phone-number-format"></span>
### Phone number format
Phone number should be displayed and stored in international format:
- prefix "+" with country calling codes (https://en.wikipedia.org/wiki/List_of_country_calling_codes)
- phone number itself without any leading 0, no spaces, no points, no dash. Just numbers.
- total max length (prefix included) cannot exceed 25 chars


<span id="lexicon-spok-instances-lifecycle"></span>
### Spok instances lifecyle
During its life, a spok instance will go through several status.
<table>
    <tr><th>Identifier</th><th>From which status</th><th>Who can set it</th><th>Description</th><th>Visitors interactions</th></tr>
    <tr><td>pending</td><td>respoked</td><td>spok's owner, lambda registered user</td><td>when a spok's instance is to be re-spoked or un-spoked</td><td>none / stacks / walls. Depends on visibility</td></tr>
    <tr><td>respoked</td><td>published</td><td>lambda registered user</td><td>user wants to re-spoke or have created and published a new spok</td><td>none / stacks / walls. Depends on visibility</td></tr>
    <tr><td>unspoked</td><td>published</td><td>lambda registered user</td><td>user un-spoke (doesn't wants to re-spoke)</td><td>none / stacks / walls. Depends on visibility</td></tr>
    <tr><td>removed</td><td>published, ok, nok</td><td>spok instance's owner, system</td><td>when a user does not want a spok appearing on its wall anymore.<br />Or when a spok is disabled (by the spok's original creator or by an administrator).</td><td>spok instance is not accessible anymore</td></tr>
</table>


<span id="lexicon-spok-instances-visibilities"></span>
### Spok instances visibilities
Spok instance can be:
<br />"public", the spok is visible to any user (stack and wall) and freely re-spoked
<br />"private", the spok is only visible to a <a href="#api-Users-group__create_">group</a> of targeted users
<br/>
<br/>The "private" visibility makes the spok instance to be a one-time private spok:
- it doesn't appear on the spoker's wall, only on targeted user's stack
- it can't be re-spoked (re-spoke and un-spoke actions are possible, but if "re-spoked" the re-spoke is not fully effective)


<span id="lexicon-notifications-types"></span>
### Notifications types
Notifications can be of different types: "respoked", "mention", "comment", "follower", "registration", "friend".


<span id="lexicon-publication-targets"></span>
### Publication's targets
When a spok is published (new published or alread existing re-spoked), users to be targeted by the spok can be specified.
<br />If not, by default, this is the spoker's followers that are targeted.
<br />if set, the spoker can targeted:
- her/his phone contacts (group called "phone's contacts")
- some custom group, in which can be indistinctly spoke-friends or phone's contacts


<span id="lexicon-resource-status"></span>
### Resource status
A resource status is indicated by

    {
        status: 0|1
    }


<span id="lexicon-spoks-types"></span>
### Spoks types
Spoks content type can be: "picture", "animatedgif", "video", "sound", "url", "rawtext", "htmltext", "poll" or "riddle".


<span id="lexicon-spok-ttl"></span>
### Spok TTL
A spok can have a TTL (default 0 means infinite).
<br />When TTL expires, spok is disabled ("enabled" flag set to false) and all its instances are in "removed" status.
<br />A TTL is set when creating a new spok and is in seconds.
<br />When viewing a spok, TTL is the spok's expiration timestamp (0 means infinite, no expiration timestamp).


<span id="lexicon-upload-files"></span>
### Upload files
There is some rules and limitations for file uploading.
<table>
    <tr><th>Name</th><th>Accepted formats</th><th>Size constraint</th><th>Format constraints</th></tr>
    <tr><td>image</td><td>Any</td><td>10MB</td><td>No</td></tr>
    <tr><td>animated gif</td><td>gif</td><td>5MB</td><td>No</td></tr>
    <tr><td>video</td><td>Any</td><td>100MB</td><td>Duration: 2sec - 6h</td></tr>
    <tr><td>sound</td><td>Any</td><td>30MB</td><td>Duration: 2sec - 90min</td></tr>
</table>
<br />For transcoding rules and supported readables media formats please read:
- iOS: https://developer.apple.com/library/ios/documentation/Miscellaneous/Conceptual/iPhoneOSTechOverview/MediaLayer/MediaLayer.html
- Android: http://developer.android.com/guide/appendix/media-formats.html


<span id="lexicon-url-content-types"></span>
### URL's content types
URL content type can be: "picture", "animatedgif", "video", "sound" or "text".
<br />If there is no content type set, default is "unknown".
<br />These item's content types also concern poll' questions and answers, and riddle's questions and answers.
<br />Their default are "text".


<span id="lexicon-url-preview"></span>
### URL's preview
URL content type preview concerns only the following content types: "animatedgif", "video" and "sound".
<br />As for URL's content types, this rule also apply to poll's questions and answers, and riddle's questions and answers.


<span id="lexicon-users-ages"></span>
### Users age
Age must be between (included) 13 and 99.


<span id="lexicon-users-genders"></span>
### Users genders
User's gender can be: "male" or "female".