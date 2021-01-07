
Pod::Spec.new do |s|
  s.name         = "react-native-ftp-jm"
  s.version      = "1.0.5"
  s.summary      = "react-native-ftp-jm"
  s.description  = <<-DESC
                  react-native-ftp-jm
                   DESC
  s.homepage     = "https://github.com/JimiPlatform/react-native-ftp-jm"
  s.license      = { :type => "MIT"}
  s.author       = 'Eafy'
  s.platform     = :ios, "9.0"
  s.source       = { :git => "https://github.com/JimiPlatform/react-native-ftp-jm.git", :tag => "v#{s.version}" }
  s.source_files  = "ios/*.{h,m,swift}"
  s.requires_arc = true
  s.dependency "React"
  s.dependency 'JMCurl', '~> 1.0.0'
  s.frameworks = 'UIKit'
  s.frameworks = 'AVFoundation'
  s.dependency 'FilesProvider', '0.26.0'
  s.dependency 'CocoaAsyncSocket', '7.6.3'

end

