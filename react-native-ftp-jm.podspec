
Pod::Spec.new do |s|
  s.name         = "react-native-ftp-jm"
  s.version      = "1.0.3"
  s.summary      = "react-native-ftp-jm"
  s.description  = <<-DESC
                  react-native-ftp-jm
                   DESC
  s.homepage     = "https://github.com/JimiPlatform/react-native-ftp-jm"
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "clannad" => "522674616@qq.com" }
  s.platform     = :ios, "9.0"
  s.source       = { :git => "https://github.com/JimiPlatform/react-native-ftp-jm.git", :tag => "#{s.version}" }
  s.source_files  = "ios/*.{h,m,swift}"
  s.requires_arc = true
  s.vendored_frameworks = 'ios/JMCurl.framework'
  s.dependency "React"
  s.frameworks = 'UIKit'
  s.frameworks = 'AVFoundation'
  s.dependency 'FilesProvider', '0.26.0'
  s.dependency 'CocoaAsyncSocket', '7.6.3'

end

