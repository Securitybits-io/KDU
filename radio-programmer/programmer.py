import argparse
import classes.radio as radio

def main():

    parser = argparse.ArgumentParser(description="TRI-152 Radio Programmer")
    parser.add_argument('-d', '--device', default="/dev/ttyUSB0", help='The device that should be programmed, default /dev/ttyUSB0')
    parser.add_argument('-c', '--config', default="config.cfg", help='The configuration file that will be programmed, default ./device.cfg')
    args = parser.parse_args()

    eud = args.device
    config_file = args.config

    tri = radio.radio(eud, config_file)
    tri.connect()
    tri.program()
    
if __name__ == "__main__":
    main()