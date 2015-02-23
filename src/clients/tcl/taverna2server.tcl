# Tcl package providing access to Taverna 2 Server release 1.
# This code also works as an executable script. Use it like this:
#
#    tclsh taverna2server.tcl http://host/taverna-server workflow.t2flow \
#            input1Name input1.file input2Name input2.file ...
#
# Dependencies:
#    Tcl    8.5
#    TclOO  0.6.2
#    tdom   0.8.2
#    base64 2.4.1
# _or_
#    Tcl    8.6
#    tdom   0.8.2
#
# Copyright (c) 2010, The University of Manchester
#
# $Id$
#

if {[package vsatisfies [package require Tcl 8.5] 8.6]} {
    # Skip requiring features that are built in to 8.6
} else {
    package require TclOO 0.6.2
    package require base64 2.4.1
}
package require http
package require tdom 0.8.2

namespace eval ::taverna2server {
	namespace path ::oo
    variable LogWADL 0
    variable SNS "http://ns.taverna.org.uk/2010/xml/server/"
    variable RNS "http://ns.taverna.org.uk/2010/xml/server/rest/"
    namespace export service

    class create RestSupportCore {
        variable base wadls acceptedmimetypestack
        constructor baseURL {
            set base $baseURL
            my LogWADL $baseURL
        }

        method ExtractError {tok} {
            return [http::code $tok],[http::data $tok]
        }

        method OnRedirect {tok location} {
            upvar 1 url url
            set url $location
            set where $location
            my LogWADL $where
            if {[string equal -length [string length $base/] $location $base/]} {
                set where [string range $where [string length $base/] end]
                return -level 2 [split $where /]
            }
            return -level 2 $where
        }

        method LogWADL url {
            variable ::taverna2server::LogWADL
            if {!$LogWADL} {
                return;# do nothing
            }
            set tok [http::geturl $url?_wadl]
            set w [http::data $tok]
            http::cleanup $tok
            if {![info exist wadls($w)]} {
                set wadls($w) 1
                puts stderr $w
            }
        }

        method PushAcceptedMimeTypes args {
            lappend acceptedmimetypestack [http::config -accept]
            http::config -accept [join $args ", "]
            return
        }
        method PopAcceptedMimeTypes {} {
            set old [lindex $acceptedmimetypestack end]
            set acceptedmimetypestack [lrange $acceptedmimetypestack 0 end-1]
            http::config -accept $old
            return
        }

        method DoRequest {method url {type ""} {value ""}} {
            for {set reqs 0} {$reqs < 5} {incr reqs} {
                if {[info exists tok]} {
                    http::cleanup $tok
                }
                set tok [http::geturl $url -method $method -type $type \
                        -query $value]
                if {[http::ncode $tok] > 399} {
                    set msg [my ExtractError $tok]
                    http::cleanup $tok
                    return -code error $msg
                } elseif {[http::ncode $tok]>299 || [http::ncode $tok]==201} {
                    set location {}
                    if {[catch {
                        set location [dict get [http::meta $tok] Location]
                    }]} {
                        http::cleanup $tok
                        error "missing a location header!"
                    }
                    my OnRedirect $tok $location
                } else {
                    set s [http::data $tok]
                    http::cleanup $tok
                    return $s
                }
            }
            error "too many redirections!"
        }

        method GET args {
            return [my DoRequest GET $base/[join $args /]]
        }

        method POST {args} {
            set type [lindex $args end-1]
            set value [lindex $args end]
            set path [join [lrange $args 0 end-2] /]
            return [my DoRequest POST $base/$path $type $value]
        }

        method PUT {args} {
            set type [lindex $args end-1]
            set value [lindex $args end]
            set path [join [lrange $args 0 end-2] /]
            return [my DoRequest PUT $base/$path $type $value]
        }

        method DELETE args {
            return [my DoRequest DELETE $base/[join $args /]]
        }
    }

    class create service {
        superclass RestSupportCore
        self {
            variable service
            method address= serviceURL {
                set service $serviceURL
            }
            method address {{suffix {}}} {
                if {$suffix ne ""} {
                    return $service/$suffix
                } else {
                    return $service
                }
            }

            method SimpleGet path {
                set tok [http::geturl [my address rest/$path]]
                set result [http::data $tok]
                http::cleanup $tok
                return $result
            }
            forward runLimit            my SimpleGet policy/runLimit
            forward permittedWorkflows  my SimpleGet policy/permittedWorkflows
            forward permittedListeners  my SimpleGet policy/permittedListeners
            method runs {} {
                variable ::taverna2server::RNS
                dom parse [my SimpleGet runs] doc
                set result {}
                foreach run [doc selectNodes -namespaces [list t2sr $RNS] \
                        "/t2sr:runs/t2sr:run"] {
                    lappend result [$run @xlink:href]
                }
                return $result
            }

            method withFile {filename as varName do script} {
                # Verify the sugar
                if {$as ne "as" || $do ne "do"} {
                    return -code error "syntax error"
                }
                upvar 1 $varName var
                set var [[self] new -file $filename]
                catch {
                    uplevel 1 $script
                } msg opt
                $var destroy
                return -options $opt $msg
            }
        }
        variable SNS RNS created
        constructor {op arg} {
            set created 0
            my eval {namespace upvar ::taverna2server SNS SNS RNS RNS}

            if {$op eq "-file"} {
                set f [open $arg]
                set t2flow [read $f]
                close $f
            } elseif {$op eq "-id"} {
                next [[self class] address rest/runs/$arg]
                return
            } else {
                return -code error "unknown operation: must be -file or -id"
            }

            dom parse $t2flow workflow
            set contents [[$workflow documentElement] asList]
            dom createDocumentNS $SNS workflow wrapped
            [$wrapped documentElement] appendFromList $contents

            set tok [http::geturl [[self class] address rest/runs] \
                    -type application/xml -query [$wrapped asXML]]
            if {[http::ncode $tok] > 399} {
                set msg [my ExtractError $tok]
                http::cleanup $tok
                return -code error $msg
            } elseif {[http::ncode $tok] < 300 && [http::ncode $tok] != 201} {
                http::cleanup $tok
                return -code error "unexpected OK"
            }
            next [dict get [http::meta $tok] Location]
            set created 1
            http::cleanup $tok
        }
        destructor {
            if {$created && [catch {my DELETE} msg]} {
                puts stderr "WARNING: $msg"
            }
        }

        method status {{status ""}} {
            if {$status eq ""} {
                return [my GET status]
            } else {
                return [my PUT status text/plain $status]
            }
        }

        method executeSynchronously {} {
            if {[my status] eq "Initialized"} {
                my status Operating
            }
            while {[my status] eq "Operating"} {
                after 1000
            }
        }

        method expiry {{expiry ""}} {
            if {$expiry eq ""} {
                set t [my GET expiry]
            } else {
                set t [my PUT expiry text/plain $expiry]
            }
            clock scan $t -format %Y-%m-%dT%H:%M:%S%z
        }

        method createTime {} {
            clock scan [my GET createTime] -format %Y-%m-%dT%H:%M:%S%z
        }
        method startTime {} {
            set t [my GET startTime]
            if {$t eq ""} return
            clock scan $t -format %Y-%m-%dT%H:%M:%S%z
        }
        method finishTime {} {
            set t [my GET finishTime]
            if {$t eq ""} return
            clock scan $t -format %Y-%m-%dT%H:%M:%S%z
        }

        method property {listener property {value ""}} {
            if {[llength [info level 0]] == 4} {
                my GET listeners $listener properties $property
            } else {
                my PUT listeners $listener properties $property text/plain $value
            }
        }

        method input {port file|value literal} {
            switch ${file|value} {
                file - value {
                    # OK
                }
                default {
                    return -code error "unknown input type"
                }
            }
            dom createDocumentNS $RNS runInput valuedoc
            set v [$valuedoc createElementNS $RNS ${file|value}]
            $v appendChild [$valuedoc createTextNode $literal]
            [$valuedoc documentElement] appendChild $v
            my PUT input input $port application/xml [$valuedoc asXML]
            return
        }

        method inputs file {
            my PUT input baclava text/plain $file
            return
        }

        method outputs file {
            my PUT output text/plain $file
        }

        method ls {{base ""}} {
            my PushAcceptedMimeTypes application/xml
            set code [catch {
                my GET wd $base
            } result opt]
            my PopAcceptedMimeTypes
            if {$code} {
                return -options $opt $result
            }

            set items {}
            dom parse $result doc
            set nsmap [list ts2 $SNS t2sr $RNS]
            foreach dir [$doc selectNodes -namespaces $nsmap \
                             "t2sr:directoryContents/t2s:dir"] {
                lappend items [$dir @name]/
            }
            foreach file [$doc selectNodes -namespaces $nsmap \
                              "t2sr:directoryContents/t2s:file"] {
                lappend items [$file @name]
            }
            return $items
        }

        method get file {
            my PushAcceptedMimeTypes application/octet-stream
            set out [my GET wd $file]
            my PopAcceptedMimeTypes
            return $out
        }

        # Helper for file operations
        method FileOp {op name {content ""}} {
            dom createDocumentNS $RNS $op doc
            set element [$doc documentElement]
            $element setAttributeNS "" xmlns:t2sr $RNS
            $element setAttributeNS $RNS t2sr:name $name
            if {[llength [info level 0]] == 5} {
                if {[info tclversion] eq "8.5"} {
                    $element appendChild [$doc createTextNode \
                            [base64::encode $content]]
                } else {
                    $element appendChild [$doc createTextNode \
                            [binary encode base64 $content]]
                }
            }
            return [$doc asXML]
        }

        method put {fileName contents} {
            set path [file split $fileName]
            #### <t2sr:upload t2sr:name="..."> base64data </t2sr:upload>
            set message [my FileOp upload [lindex $path end] $contents]
            return [join [lrange [my POST wd {*}[lrange $path 0 end-1] \
                    application/xml $message] 1 end] "/"]
        }

        method mkdir {dirName} {
            set path [file split $dirName]
            #### <t2sr:mkdir t2sr:name="..."/>
            set message [my FileOp mkdir [lindex $path end]]
            return [join [lrange [my POST wd {*}[lrange $path 0 end-1] \
                    application/xml $message] 1 end] "/"]
        }
    }
}

package provide taverna2server 1.0

# Demonstration code
if {[info script] ne $::argv0} {
    return
}

namespace eval sample-code {
    proc ReadBinaryFile filename {
        set f [open $filename]
        fconfigure $f -translation binary
        set data [read $f]
        close $f
        return $data
    }
    proc WriteBinaryFile {filename data} {
        set f [open $filename w]
        fconfigure $f -translation binary
        puts -nonewline $f $data
        close $f
    }

    namespace import taverna2server::service
    service address= [lindex $argv 0]
    service withFile [lindex $argv 1] as run do {
        $run mkdir in
        foreach {name filename} [lrange $argv 2 end] {
            # Upload the file to a synthetic name
            $run input $name file [$run put in/f[incr i] \
                    [ReadBinaryFile $filename]]
        }
        $run executeSynchronously
        puts STDOUT:\t[$run property io stdout]
        puts STDERR:\t[$run property io stderr]
        puts EXIT:\t[$run property io exitcode]
        foreach filename [$run ls out] {
            puts FILE:\t$filename
            # Ignore subdirectories
            if {[string match */ $filename]} continue
            # Download the file
            WriteBinaryfile [file tail $filename] [$run get out/$filename]
        }
    }
}

return

# Local Variables:
# mode: tcl
# indent-tabs-mode: nil
# End:
