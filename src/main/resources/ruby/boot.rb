require 'ruby/notifier'

class Boot
  def start(o)
    Notifier.notify(o['email'], o['password'], o['domain'], o['room_name'], o['message'] )
  end
end
