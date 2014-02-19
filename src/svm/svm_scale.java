package svm;
import android.util.Log;
import audio.feature.WindowFeature;
import edu.virginia.stk4zn.Static;

import java.io.*;
import java.util.*;
import java.text.DecimalFormat;

public class svm_scale
{
	private String line = null;
	private double lower = -1.0;
	private double upper = 1.0;
	private double y_lower;
	private double y_upper;
	private boolean y_scaling = false;
	private double[] feature_max;
	private double[] feature_min;
	private double y_max = -Double.MAX_VALUE;
	private double y_min = Double.MAX_VALUE;
	private int max_index;
	private long num_nonzeros = 0;
	private long new_num_nonzeros = 0;
	
	private DataOutputStream outputStream;
    
   
    public svm_scale(){
    }

	private static void exit_with_help()
	{
		System.out.print(
		 "Usage: svm-scale [options] data_filename\n"
		+"options:\n"
		+"-l lower : x scaling lower limit (default -1)\n"
		+"-u upper : x scaling upper limit (default +1)\n"
		+"-y y_lower y_upper : y scaling limits (default: no y scaling)\n"
		+"-s save_filename : save scaling parameters to save_filename\n"
		+"-r restore_filename : restore scaling parameters from restore_filename\n"
		);
		System.exit(1);
	}

	private BufferedReader rewind(BufferedReader fp, String filename) throws IOException
	{
		fp.close();
		return new BufferedReader(new FileReader(filename));
	}

	private void output_target(double value) throws IOException
	{
		if(y_scaling)
		{
			if(value == y_min)
				value = y_lower;
			else if(value == y_max)
				value = y_upper;
			else
				value = y_lower + (y_upper-y_lower) *
				(value-y_min) / (y_max-y_min);
		}
		outputStream.writeBytes(value + " ");
		System.out.print(value + " ");
	}

	private void output(int index, double value) throws IOException
	{
		/* skip single-valued attribute */
		if(feature_max[index] == feature_min[index])
			return;

		if(value == feature_min[index])
			value = lower;
		else if(value == feature_max[index])
			value = upper;
		else
			value = lower + (upper-lower) * 
				(value-feature_min[index])/
				(feature_max[index]-feature_min[index]);

		if(value != 0)
		{
			outputStream.writeBytes(index + ":" + value + " ");
			System.out.print(index + ":" + value + " ");
			new_num_nonzeros++;
		}
	}

	private String readline(BufferedReader fp) throws IOException
	{
		line = fp.readLine();
		return line;
	}


	public void run(String []argv) throws IOException
	{
		int index;
		BufferedReader fp = null;

		String data_filename = argv[0];

        String scaled_filename = null;

        if (argv.length == 2){
            scaled_filename = argv[1];
        } else{
            scaled_filename = Static.getScaledTrainingFilepath();
        }


		if(!(upper > lower) || (y_scaling && !(y_upper > y_lower)))
		{
			Log.d(Static.DEBUG,"inconsistent lower/upper specification");
			System.exit(1);
		}


		if(argv.length != 2){
            Log.d(Static.DEBUG,"improper syntax");
			exit_with_help();
        }
		try {
            fp = new BufferedReader(new FileReader(data_filename));
		} catch (Exception e) {
			Log.d(Static.DEBUG, "can't open file " + data_filename);
			System.exit(1);
		}

		/* assumption: min index of attributes is 1 */
		/* pass 1: find out max index of attributes */
		max_index = 0;


		while (readline(fp) != null)
		{
			StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");
			st.nextToken();
			while(st.hasMoreTokens())
			{
				index = Integer.parseInt(st.nextToken());
				max_index = Math.max(max_index, index);
				st.nextToken();
				num_nonzeros++;
			}
		}

		try {
			feature_max = new double[(max_index+1)];
			feature_min = new double[(max_index+1)];
		} catch(OutOfMemoryError e) {
			Log.d(Static.DEBUG,"can't allocate enough memory");
			System.exit(1);
		}


        for(int i=0;i<=max_index;i++)
		{
			feature_max[i] = -Double.MAX_VALUE;
			feature_min[i] = Double.MAX_VALUE;
		}


        fp = rewind(fp, data_filename);

		/* pass 2: find out min/max value */
		while(readline(fp) != null)
		{
			int next_index = 1;
			double target;
			double value;

			StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");
			target = Double.parseDouble(st.nextToken());
			y_max = Math.max(y_max, target);
			y_min = Math.min(y_min, target);

			while (st.hasMoreTokens())
			{
				index = Integer.parseInt(st.nextToken());
				value = Double.parseDouble(st.nextToken());

				for (int i = next_index; i<index; i++)
				{
					feature_max[i] = Math.max(feature_max[i], 0);
					feature_min[i] = Math.min(feature_min[i], 0);
				}

				feature_max[index] = Math.max(feature_max[index], value);
				feature_min[index] = Math.min(feature_min[index], value);
				next_index = index + 1;
			}

			for(int i=next_index;i<=max_index;i++)
			{
				feature_max[i] = Math.max(feature_max[i], 0);
				feature_min[i] = Math.min(feature_min[i], 0);
			}
		}


        fp = rewind(fp, data_filename);


        outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(scaled_filename)));
		
		while(readline(fp) != null)
		{
			int next_index = 1;
			double target;
			double value;
			

			StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");
			target = Double.parseDouble(st.nextToken());
			output_target(target);
			while(st.hasMoreElements())
			{
				index = Integer.parseInt(st.nextToken());
				value = Double.parseDouble(st.nextToken());
				for (int i = next_index; i<index; i++)
					output(i, 0);
				output(index, value);
				next_index = index + 1;
			}

			for(int i=next_index;i<= max_index;i++)
				output(i, 0);
			System.out.print("\n");
			outputStream.writeBytes("\n");
		}
		if (new_num_nonzeros > num_nonzeros)
			Log.d(Static.DEBUG,
			 "WARNING: original #nonzeros " + num_nonzeros+"\n"
			+"         new      #nonzeros " + new_num_nonzeros+"\n"
			+"Use -l 0 if many original feature values are zeros\n");
		outputStream.close();
		fp.close();
	}

	public static void main(String argv[]) throws IOException
	{
		String[] args = {"-s","scaled_training","training"};
		svm_scale s = new svm_scale();
		s.run(args);
	}
}
