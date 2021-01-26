
Pod::Spec.new do |s|
  s.name         = "react-native-ftp-jm"
  s.version      = "1.0.7"
  s.summary      = "react-native-ftp-jm"
  s.description  = <<-DESC
                  react-native-ftp-jm
                   DESC
  s.homepage     = "https://github.com/JimiPlatform/react-native-ftp-jm"
  s.license      = { :type => "MIT"}
  s.author       = 'Eafy'
  s.platform     = :ios, "9.0"
  s.source       = { :git => "https://github.com/JimiPlatform/react-native-ftp-jm.git", :tag => "v#{s.version}" }
  s.source_files  = "ios/*.{h,m}"
  s.requires_arc = true
  s.dependency "React"
  s.dependency 'JMSmartFTPUtils', '1.0.2'
  s.dependency 'CocoaAsyncSocket'

end

